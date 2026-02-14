package coop.local.scheduler;

import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.CommandCompleteEvent;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.RxOpenEvent;
import coop.local.Invokable;
import coop.local.database.downlink.Downlink;
import coop.local.listener.EventListener;
import coop.local.EventPayload;
import coop.local.database.job.Job;
import coop.local.database.job.JobRepository;
import coop.local.database.job.JobStatus;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scheduler implements EventListener, Invokable {
    private static final Duration PURGE_FREQUENCY = Duration.ofHours(6);

    private final JobRepository jobRepository;
    private final DownlinkDispatcher downlinkDispatcher;
    private final ResourceManager resourceManager;

    private boolean isInitialized = false;
    private final Set<String> dedupeKeys = new HashSet<>();
    private long nextPurge = 0;

    public Scheduler(LocalStateProvider stateProvider,
                     JobRepository jobRepository,
                     DownlinkDispatcher downlinkDispatcher) {

        this.jobRepository = jobRepository;
        this.resourceManager = new ResourceManager(stateProvider);
        this.downlinkDispatcher = downlinkDispatcher;
    }

    @Override
    public synchronized void invoke() {

        // TODO: To reduce the liklihood that a job hogs a resource, we should force it to send a heartbeat and then
        //       expire after a short period. Right now the resource expires after 6 hours no matter what but we can
        //       reduce that by sending a heartbeat every 15 minutes and expiring after 30.


        /**
         * What is the benefit of this queue? What is it actually getting us? All we're doing is blasting events
         * to people who aren't listening. Perhaps instead we should wait and respond to the RX event. We can continue
         * to reserve space, but the circular queue is unnecessary.
         */


        if(!isInitialized) {
            purgeIfNecessary();
            hydrate();
            isInitialized = true;
        }

        expireReservations();
        createReservations();
        purgeIfNecessary();
    }

    private void expireReservations() {

        //TODO: Devices have 30 minutes to check in, otherwise they go to the back of the line
        List<Job> expiredReservations = jobRepository.findReservedJobsOlderThan(Duration.ofMinutes(30));
        for(Job reserved : expiredReservations) {
            reserved.setStatus(JobStatus.CREATED);
            resourceManager.stopConsuming(reserved);
            jobRepository.unreserve(reserved);

            // Check if the change stuck, otherwise it's probably processing.
            if(JobStatus.CREATED.equals(reserved.getStatus())) {

                // Expire any jobs that expired while they were reserved.
                if(reserved.isExpired()) {
                    reserved.setStatus(JobStatus.FAILED);
                    jobRepository.updateStatus(reserved);
                }
            }
        }
    }

    private void createReservations() {
        for(Job job : jobRepository.findCreatedJobs()) {
            if(resourceManager.tryToConsume(job)) {
                job.setStatus(JobStatus.RESERVED);
                jobRepository.updateStatus(job);
            }
        }
    }

    private void sendToDispatcher(Job job) {
        job.setStatus(JobStatus.PENDING);
        jobRepository.updateStatus(job);
        downlinkDispatcher.add(message(job));
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
            rxOpenListener(payload, event);
        }
    }

    /**
     * In case of a shutdown, hydrate the queue with what was stored to disk
     */
    private void hydrate() {

        // Find any jobs that have been created but not yet allocated
        List<Job> createdJobs = jobRepository.findCreatedJobs();
        for(Job job : createdJobs) {
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

        // Find any jobs that have been dispatched and resubmit them
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

    public synchronized void rxOpenListener(EventPayload payload, RxOpenEvent event) {
        if(payload.getComponent() == null) {
            return;
        }

        // If the job is already reserving resources, then submit it
        Job reserved = jobRepository.findReserved(payload.getComponent().getComponentId());
        if(reserved != null) {
            sendToDispatcher(reserved);
        }

        // If the job is created, check if it can consume resources and submit if so
        Job created = jobRepository.findByStatus(payload.getComponent().getComponentId(), JobStatus.CREATED);
        if(created != null) {
            if(resourceManager.tryToConsume(created)) {
                sendToDispatcher(created);
            }
        }
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
            if (dedupeKey != null) {
                dedupeKeys.add(dedupeKey);
            }

            if(resourceManager.tryToConsume(job)) {
                sendToDispatcher(job);
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
