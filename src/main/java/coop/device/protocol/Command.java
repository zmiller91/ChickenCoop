package coop.device.protocol;

public interface Command {
    DownlinkFrame getCommand(String serialNumber);
    boolean isValid();
}
