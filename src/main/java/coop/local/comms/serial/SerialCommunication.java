package coop.local.comms.serial;

public interface SerialCommunication {
    boolean bytesAvailable();
    byte[] readBytes();
    int write(byte[] bytes);
}
