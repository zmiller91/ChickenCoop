package coop.device.protocol.command;

import coop.device.protocol.DownlinkFrame;

import java.util.Map;

public interface Command {
    DownlinkFrame getCommand(String serialNumber, Map<String, String> params);
    boolean isValid(Map<String, String> params);
}
