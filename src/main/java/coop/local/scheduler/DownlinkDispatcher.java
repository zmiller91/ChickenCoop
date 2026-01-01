package coop.local.scheduler;

import coop.device.protocol.event.AckEvent;
import coop.device.protocol.event.Event;
import coop.local.Invokable;
import coop.local.listener.EventListener;
import coop.local.EventPayload;
import coop.local.comms.Communication;
import coop.local.comms.message.MessageSent;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * This class manages the commands that are sent to devices. This class will only send one actionKey at a time, only
 * moving onto the next one if the in-flight message has been acked.
 */
@Log4j2
public class DownlinkDispatcher implements EventListener, Invokable {

    private static final Duration INFLIGHT_EXPIRY = Duration.ofMinutes(1);
    private static final int MAX_TX_ATTEMPTS = 3;

    //TODO: This is hardcoded. Probably shouldn't be, but it is.
    private static final int DEVICE_ADDR = 32;

    private QueueEntry inFlight = null;
    private final Queue<QueueEntry> queue = new ConcurrentLinkedQueue<>();
    private final Communication communication;

    public DownlinkDispatcher(Communication communication) {
        this.communication = communication;
    }

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return Collections.singletonList(AckEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {
        if(!(payload.getEvent() instanceof AckEvent event)) {
            return;
        }

        if(inFlight != null && inFlight.getMessage().getDownlink().getFrameId().equals(event.getFrameId())) {
            callback(inFlight.getMessage(), inFlight.getMessage().getOnAckSuccess());
            inFlight = null;
        }
    }

    public void add(OutboundMessage message){
        queue.add(new QueueEntry(message));
    }

    private QueueEntry getMessageToProcess() {
        // First check to see if there is a message inFlight and if there isn't then return the next message in the queue
        if(inFlight == null) {
            return queue.poll();
        }

        // Otherwise check if the inFlight has succeeded, if it has then check to see if it has timed out waiting
        // for an ack. If it has, then fail the job and return the next message in the queue.
        if(inFlight.txSucceeded() && inFlight.hasFlightExpired()) {
            callback(inFlight.getMessage(), inFlight.getMessage().getOnAckFailure());
            inFlight = null;
            return queue.poll();
        }

        // Otherwise continue to process the currently inflight message
        return inFlight;
    }

    @Override
    public void invoke() {

        // Get the entry that is to be processed. Either the current one if it hasn't completed or the next in line.
        inFlight = getMessageToProcess();
        if(inFlight == null) {
            return;
        }

        if(!inFlight.txSucceeded()) {
            inFlight.addAttempt();
            MessageSent sent = communication.write(DEVICE_ADDR, inFlight.getMessage().getDownlink().getFrame());

            // If the message was successfully transmitted, then stick around for the ack otherwise move on.
            if (sent.isSuccess()) {
                inFlight.markSuccessfulTx();
                callback(inFlight.getMessage(), inFlight.getMessage().getOnTxSuccess());
                if(!inFlight.message.getDownlink().isRequiresAck()) {
                    callback(inFlight.getMessage(), inFlight.getMessage().getOnAckIgnored());
                }

            // Don't keep retrying. Give it a few tries and then move on.
            } else if (inFlight.hasExceededMaxAttempts()) {
                callback(inFlight.getMessage(), inFlight.getMessage().getOnTxFailure());
                inFlight = null;
            }
        }
    }

    private void callback(OutboundMessage message, Consumer<OutboundMessage> cbck) {
        if (cbck != null) {
            try {
                cbck.accept(message);
            } catch (Exception e){
                log.error("Failed to call callback for message: {}", message.getDownlink().getFrameId(), e);
            }
        }
    }

    private static class QueueEntry {

        private long inFlightTime = Long.MAX_VALUE;
        private final OutboundMessage message;
        private int attempts;
        private boolean txSucceeded = false;

        private QueueEntry(OutboundMessage message) {
            this.message = message;
            this.attempts = 0;
        }

        public OutboundMessage getMessage() {
            return message;
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

        private boolean hasExceededMaxAttempts() {
            return attempts >= MAX_TX_ATTEMPTS;
        }

        private boolean hasFlightExpired() {
            return (System.currentTimeMillis() - inFlightTime) > INFLIGHT_EXPIRY.toMillis();
        }

    }
}
