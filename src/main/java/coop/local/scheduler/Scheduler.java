package coop.local.scheduler;

import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.CommandCompleteEvent;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.RxOpenEvent;
import coop.local.Invokable;
import coop.local.database.downlink.Downlink;
import coop.local.database.downlink.DownlinkRepository;
import coop.local.listener.EventListener;
import coop.local.EventPayload;
import coop.local.comms.Communication;
import coop.local.database.job.Job;
import coop.local.database.job.JobRepository;
import coop.local.database.job.JobStatus;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
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

public class Scheduler implements EventListener, Invokable {
    private static final Duration PURGE_FREQUENCY = Duration.ofHours(6);

    private final JobRepository jobRepository;
    private final DownlinkDispatcher downlinkDispatcher;
    private final ResourceManager resourceManager;
    private final DownlinkRepository downlinkRepository;

    private final Set<String> dedupeKeys = new HashSet<>();
    private final CircularBuffer<Job> buffer = new CircularBuffer<>();
    private long nextPurge = 0;

    public Scheduler(LocalStateProvider stateProvider,
                     JobRepository jobRepository,
                     DownlinkDispatcher downlinkDispatcher,
                     DownlinkRepository downlinkRepository) {

        this.jobRepository = jobRepository;
        this.resourceManager = new ResourceManager(stateProvider);
        this.downlinkDispatcher = downlinkDispatcher;
        this.downlinkRepository = downlinkRepository;
        purgeIfNecessary();
        hydrate();
    }

    @Override
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
                job.setStatus(JobStatus.PENDING);
                jobRepository.updateStatus(job);
                downlinkDispatcher.add(message(job));
            }
        }

        purgeIfNecessary();
    }

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return List.of(CommandCompleteEvent.class, RxOpenEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {
        if(payload.getEvent() instanceof CommandCompleteEvent event) {
            completionListener(event);
        }

        if(payload.getEvent() instanceof RxOpenEvent event) {
            rxOpenListener(event);
        }
    }

    /**
     * In case of a shutdown, hydrate the queue with what was stored to disk
     */
    private void hydrate() {

        // Find any jobs that have been created but not yet allocated
        List<Job> createdJobs = jobRepository.findCreatedJobs();
        for(Job job : createdJobs) {
            buffer.add(job);
            if(job.getDedupeKey() != null) {
                dedupeKeys.add(job.getDedupeKey());
            }
        }

        // Find any jobs that have been allocated and re-allocate their resource consumption
        List<Job> runningJobs = jobRepository.findJobsUsingResources();
        for(Job job : runningJobs) {
            resourceManager.forceConsumption(job);
            if(job.getDedupeKey() != null) {
                dedupeKeys.add(job.getDedupeKey());
            }
        }

        // Find any jobs that have been submitted for the queue and resubmit them
        List<Job> jobs = jobRepository.findPendingJobs();
        for(Job job : jobs) {
            downlinkDispatcher.add(message(job));
        }

        // Not a lot we can do here. We can't request a status. Just mark it as waiting for completion.
        Job waitingForAck = jobRepository.findWaitingForAck();
        if(waitingForAck != null) {
            waitingForAck.setStatus(JobStatus.WAITING_FOR_COMPLETE);
            jobRepository.updateStatus(waitingForAck);
        }
    }

    public synchronized void completionListener(CommandCompleteEvent event) {

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

    public synchronized void rxOpenListener(RxOpenEvent event) {

    }

    public synchronized boolean create(ComponentState component, DownlinkFrame frame, String dedupeKey) {

        if(dedupeKey != null && dedupeKeys.contains(dedupeKey)) {
            return false;
        }

        Job job = new Job();
        job.setCreatedAt(System.currentTimeMillis());
        job.setStatus(JobStatus.CREATED);
        job.setDownlink(Downlink.from(frame));
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


    /**
     * Component ACKed back. Waiting for completion signal.
     */
    public synchronized void txAckedCallback(OutboundMessage message) {
        if(!(message.getContext() instanceof Job job)) {
            return;
        }

        job.setStatus(JobStatus.WAITING_FOR_COMPLETE);
        jobRepository.updateStatus(job);
    }

    /**
     * Transmission successful. Waiting for acknowledgement.
     */
    public synchronized void txSuccessCallback(OutboundMessage message) {
        if(!(message.getContext() instanceof Job job)) {
            return;
        }

        job.setStatus(JobStatus.WAITING_FOR_ACK);
        jobRepository.updateStatus(job);
    }

    /**
     * Transmission failed.
     */
    public synchronized void failureCallback(OutboundMessage message) {
        if(!(message.getContext() instanceof Job job)) {
            return;
        }

        job.setStatus(JobStatus.FAILED);
        jobRepository.updateStatus(job);
        if(job.getDedupeKey() != null) {
            dedupeKeys.remove(job.getDedupeKey());
        }

        resourceManager.stopConsuming(job);
    }

    /**
     * Convenience method to convert a Job into an OutboundMessage.
     *
     * @param job to convert
     * @return OutboundMessage job
     */
    private OutboundMessage message(Job job) {
        OutboundMessage message = new OutboundMessage(job.getDownlink());
        message.setContext(job);
        message.setOnTxFailure(this::failureCallback);
        message.setOnAckFailure(this::failureCallback);
        message.setOnTxSuccess(this::txSuccessCallback);
        message.setOnAckSuccess(this::txAckedCallback);
        return message;
    }

    /**
     * Purges the database of old jobs. Used to to minimize disk usage and the buildup of old jobs.
     */
    private void purgeIfNecessary() {
        long now = System.currentTimeMillis();
        if (now > nextPurge) {
            jobRepository.purge();
            nextPurge = now + PURGE_FREQUENCY.toMillis();
        }
    }
}
