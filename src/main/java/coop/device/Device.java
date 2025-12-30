package coop.device;

import coop.device.protocol.parser.EventParser;

public interface Device {
    String getDescription();
    EventParser getEventParser();
    ConfigKey[] getConfig();
}
