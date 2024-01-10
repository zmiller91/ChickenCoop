package coop.local.comms.message;

import lombok.Getter;

@Getter
public class MessageSent {

    private final int address;
    private final String message;
    private MessageSuccess success;
    private MessageError error;
    private boolean timedOut;

    public MessageSent(int address, String message) {
        this.address = address;
        this.message = message;
    }

    public byte[] serialize() {
        return String.format("AT+SEND=%s,%s,%s\r\n", address, message.length(), message).getBytes();
    }

    public void setSuccess(MessageSuccess success) {
        this.success = success;
    }

    public void setError(MessageError error) {
        this.error = error;
    }

    public boolean inProgress() {
        return this.success == null && this.error == null && !this.timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public boolean isSuccess() {
        return success != null;
    }
}
