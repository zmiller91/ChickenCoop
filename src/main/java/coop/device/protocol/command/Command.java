package coop.device.protocol.command;

import coop.device.protocol.DownlinkFrame;

public interface Command {
    DownlinkFrame getCommand(String serialNumber);
    boolean isValid();
}
