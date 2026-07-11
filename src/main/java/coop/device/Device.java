package coop.device;

import coop.device.protocol.parser.EventParser;

public interface Device {
    String getDescription();
    EventParser getEventParser();
    ConfigKey[] getConfig();

    /**
     * Config keys that apply per-port rather than to the whole component (e.g. a valve's default
     * duration/manual cutoff, which can differ per zone). Empty by default so sensor types don't need to
     * implement it.
     */
    default ConfigKey[] getPortConfig() {
        return new ConfigKey[0];
    }
}
