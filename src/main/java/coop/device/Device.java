package coop.device;

import coop.device.protocol.EventParser;

public interface Device {
    String getDescription();
    EventParser getEventParser();
    ConfigKey[] getConfig();
}
