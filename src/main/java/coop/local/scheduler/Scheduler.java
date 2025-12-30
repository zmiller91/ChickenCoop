package coop.local.scheduler;

import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.CommandCompleteEvent;
import coop.local.EventListener;
import coop.local.EventPayload;
import coop.local.comms.Communication;
import coop.local.database.Job;
import coop.local.database.JobRepository;
import coop.local.database.JobStatus;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: I don't think this scheduler is totally right. It works and it may be fine, but it may also result in a
//       component being starved. A component may shut off it's radio before it ever gets it's command due to resource
//       contention, then when it requests a command again it may experience the same behavior.
//       -
//       We may need to implement some process where rules are created and if they can't immediately be executed they
//       are stored and we respond to the component with an ACK. The rule can then be placed in a queue and optimistically
//       consume resources. Then when the device requests a command again it can immediately be started.
//       -
//       The issue I forsee is water valve commands having a longer duration than a device radio stays on. Asking the
//       device to listen for long periods of time will drain the battery.

public class Scheduler {
    private static final Duration PURGE_FREQUENCY = Duration.ofHours(6);

    private JobRepository jobRepository;
    private final CommandQueue commandQueue;
    private final ResourceManager resourceManager;

    private final Set<String> dedupeKeys = new HashSet<>();
    private final CircularBuffer<Job> buffer = new CircularBuffer<>();
    private long nextPurge = 0;

    public Scheduler(LocalStateProvider stateProvider,
                     Communication communication,
                     JobRepository jobRepository) {

        this.jobRepository = jobRepository;
        resourceManager = new ResourceManager(stateProvider);
        commandQueue = new CommandQueue(communication, jobRepository, this::successCallback, this::failedCallback);
        purgeIfNecessary();
        hydrate();
        EventListener.addListener(CommandCompleteEvent.class, this::completionListener);
    }

    public synchronized void successCallback(Job job) {}

    public synchronized void failedCallback(Job job) {
        if(job.getDedupeKey() != null) {
            dedupeKeys.remove(job.getDedupeKey());
        }

        resourceManager.stopConsuming(job);
    }

    /**
     * In case of a shutdown, hydrate the queue with what was stored to disk
     */
    private void hydrate() {
        List<Job> createdJobs = jobRepository.findCreatedJobs();
        for(Job job : createdJobs) {
            buffer.add(job);
            if(job.getDedupeKey() != null) {
                dedupeKeys.add(job.getDedupeKey());
            }
        }

        List<Job> runningJobs = jobRepository.findJobsUsingResources();
        for(Job job : runningJobs) {
            resourceManager.forceConsumption(job);
            if(job.getDedupeKey() != null) {
                dedupeKeys.add(job.getDedupeKey());
            }
        }
    }

    public synchronized void completionListener(EventPayload payload) {
        if(!(payload.getEvent() instanceof CommandCompleteEvent event)) {
            return;
        }

        Job job = jobRepository.findByFrameId(event.getMessageId());
        if(job != null) {

            if(job.getDedupeKey() != null) {
                dedupeKeys.remove(job.getDedupeKey());
            }

            resourceManager.stopConsuming(job);
            job.setStatus(JobStatus.COMPLETE);
            jobRepository.updateStatus(job);
        }

    }

    public synchronized boolean create(ComponentState component, DownlinkFrame frame, String dedupeKey) {

        if(dedupeKey != null && dedupeKeys.contains(dedupeKey)) {
            return false;
        }

        Job job = new Job();
        job.setFrameId(frame.getId());
        job.setCreatedAt(System.currentTimeMillis());
        job.setStatus(JobStatus.CREATED);
        job.setCommand(frame.toString());
        job.setDedupeKey(dedupeKey);
        job.setComponentId(component.getComponentId());

        //TODO: Make this configurable?
        job.setExpireAt(System.currentTimeMillis() + Duration.ofHours(2).toMillis());

        try {

            jobRepository.persist(job);
            jobRepository.flush(); // must call flush for the constraint exception to happen
            buffer.add(job);
            if (dedupeKey != null) {
                dedupeKeys.add(dedupeKey);
            }

            return true;

        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    public synchronized void invoke() {
        Job job = buffer.next();
        if(job != null) {

            if(job.isExpired()) {
                job.setStatus(JobStatus.FAILED);
                jobRepository.updateStatus(job);
                buffer.remove();
                if(job.getDedupeKey() != null) {
                    dedupeKeys.remove(job.getDedupeKey());
                }
            }

            else if(resourceManager.tryToConsume(job)) {
                buffer.remove();
                commandQueue.add(job);
            }
        }

        commandQueue.sendNext();
        purgeIfNecessary();
    }

    private void purgeIfNecessary() {
        long now = System.currentTimeMillis();
        if (now > nextPurge) {
            jobRepository.purge();
            nextPurge = now + PURGE_FREQUENCY.toMillis();
        }
    }
}
