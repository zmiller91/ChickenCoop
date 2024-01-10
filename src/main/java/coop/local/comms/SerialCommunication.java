package coop.local.comms;

public interface SerialCommunication {
    boolean bytesAvailable();
    byte[] readBytes();
    int write(byte[] bytes);
}
