package coop.local.scheduler;

import coop.device.protocol.event.AckEvent;
import coop.local.EventListener;
import coop.local.EventPayload;
import coop.local.comms.Communication;
import coop.local.comms.message.MessageSent;
import coop.local.database.Job;
import coop.local.database.JobRepository;
import coop.local.database.JobStatus;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * This class manages the commands that are sent to devices. This class will only send one command at a time, only
 * moving onto the next one if the in-flight message has been acked.
 */
public class CommandQueue {

    private static final Duration INFLIGHT_EXPIRY = Duration.ofMinutes(1);
    private static final int RETRIES = 3;

    private QueueEntry inFlight = null;
    private final Queue<QueueEntry> queue = new LinkedList<>();
    private final Communication communication;
    private final JobRepository jobRepository;

    private Consumer<Job> successCallback;
    private Consumer<Job> failedCallback;

    public CommandQueue(Communication communication,
                        JobRepository jobRepository,
                        Consumer<Job> successCallback,
                        Consumer<Job> failedCallback) {

        this.communication = communication;
        this.jobRepository = jobRepository;
        this.successCallback = successCallback;
        this.failedCallback = failedCallback;
        EventListener.addListener(AckEvent.class, this::ack);
        hydrate();
    }

    /**
     * In case of shutdown hydrate the queue
     */
    private void hydrate() {
        List<Job> jobs = jobRepository.findPendingJobs();
        for(Job job : jobs) {
            queue.add(new QueueEntry(job));
        }

        Job job = jobRepository.findWaitingForAck();
        if(job != null) {
            inFlight = new QueueEntry(job);
            inFlight.markSuccessfulTx();
        }
    }

    public void add(Job job){
        queue.add(new QueueEntry(job));
        job.setStatus(JobStatus.PENDING);
        jobRepository.updateStatus(job);
    }

    public void ack(EventPayload payload) {
        if(!(payload.getEvent() instanceof AckEvent event)) {
            return;
        }

        if(inFlight != null && inFlight.job.getFrameId().equals(event.getMessageId())) {
            inFlight.job.setStatus(JobStatus.WAITING_FOR_COMPLETE);
            jobRepository.updateStatus(inFlight.job);
            successCallback.accept(inFlight.job);
            inFlight = null;
        }
    }

    private QueueEntry getMessageToProcess() {
        // First check to see if there is a message inFlight and if there isn't then return the next message in the queue
        if(inFlight == null) {
            return queue.poll();
        }

        // Otherwise check if the inFlight has succeeded, if it has then check to see if it has timed out waiting
        // for an ack. If it has, then fail the job and return the next message in the queue.
        if(inFlight.txSucceeded() && inFlight.hasFlightExpired()) {
            failJob(inFlight.job);
            inFlight = null;
            return null;
        }

        // Otherwise continue to process the currently inflight message
        return inFlight;
    }

    public void sendNext() {

        // Get the entry that is to be processed. Either the current one if it hasn't completed or the next in line.
        inFlight = getMessageToProcess();
        if(inFlight == null) {
            return;
        }

        if(!inFlight.txSucceeded()) {
            inFlight.addAttempt();
            MessageSent sent = communication.write(32, inFlight.job.getCommand());

            // If the message was successfully transmitted, then stick around for the ack otherwise move on.
            if (sent.isSuccess()) {
                inFlight.markSuccessfulTx();
                inFlight.job.setStatus(JobStatus.WAITING_FOR_ACK);
                jobRepository.updateStatus(inFlight.job);

            // Don't keep retrying. Give it a few tries and then move on.
            } else if (inFlight.hasExceededRetries()) {
                failJob(inFlight.job);
                inFlight = null;
            }
        }
    }

    private void failJob(Job job) {
        job.setStatus(JobStatus.FAILED);
        jobRepository.updateStatus(job);
        failedCallback.accept(job);
    }

    private static class QueueEntry {

        private long inFlightTime = Long.MAX_VALUE;
        private final Job job;
        private int attempts;
        private boolean txSucceeded = false;

        private QueueEntry(Job job) {
            this.job = job;
            this.attempts = 0;
        }

        private void markSuccessfulTx() {
            txSucceeded = true;
            inFlightTime = System.currentTimeMillis();
        }

        private boolean txSucceeded() {
            return txSucceeded;
        }

        private void addAttempt() {
            this.attempts += 1;
        }

        private boolean hasExceededRetries() {
            return attempts >= RETRIES;
        }

        private boolean hasFlightExpired() {
            return (System.currentTimeMillis() - inFlightTime) > INFLIGHT_EXPIRY.toMillis();
        }

    }
}
