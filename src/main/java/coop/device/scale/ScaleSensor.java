package coop.device.scale;

import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.protocol.EventParser;
import coop.device.Sensor;

public class ScaleSensor implements Device, Sensor {
    @Override
    public String getDescription() {
        return "Weight Monitor";
    }

    @Override
    public EventParser getEventParser() {
        return new ScaleMessageParser();
    }

    @Override
    public ConfigKey[] getConfig() {
        return new ConfigKey[]{};
    }
}
