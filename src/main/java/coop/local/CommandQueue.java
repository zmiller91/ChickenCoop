package coop.local;

import coop.device.protocol.DownlinkFrame;
import coop.local.comms.Communication;
import coop.local.comms.message.MessageSent;
import org.apache.commons.lang3.ObjectUtils;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class manages the commands that are sent to devices. This class will only send one command at a time, only
 * moving onto the next one if the in-flight message has been acked.
 */
public class CommandQueue {

    private QueueEntry inFlightFrame = null;
    private final Queue<QueueEntry> queue = new LinkedList<>();
    private final Communication communication;

    public CommandQueue(Communication communication) {
        this.communication = communication;
    }

    public void add(DownlinkFrame frame){
        queue.add(new QueueEntry(frame));
    }

    public void ack(String messageId) {
        if(inFlightFrame != null && inFlightFrame.frame.getId().equals(messageId)) {
            inFlightFrame = null;
        }
    }

    public void sendNext() {

        QueueEntry next = inFlightFrame;
        if(next == null) {
            next = queue.poll();
        }

        // We're waiting on an ack so skip if we haven't received it
        if(next == null || next.hasSucceeded()) {
            return;
        }

        next.addAttempt();
        MessageSent sent = communication.write(32, next.frame.toString());

        // If the message was successfully transmitted, then stick around for the ack otherwise move on.
        if(sent.isSuccess()) {
            next.markSuccess();
            inFlightFrame = next.frame.getRequiresAck() ? next : null;

        // Don't keep retrying. Give it a few tries and then move on.
        } else if(next.hasExceededRetries()) {
            inFlightFrame = null;
        }
    }

    private static class QueueEntry {

        private final long insertedTime;
        private final DownlinkFrame frame;
        private int attempts;
        private boolean success = false;

        private QueueEntry(DownlinkFrame frame) {
            this.insertedTime = System.currentTimeMillis();
            this.frame = frame;
            this.attempts = 0;
        }

        private void markSuccess() {
            success = true;
        }

        private boolean hasSucceeded() {
            return success;
        }

        private void addAttempt() {
            this.attempts += 1;
        }

        private boolean hasExceededRetries() {
            return attempts >= 3;
        }

    }
}
