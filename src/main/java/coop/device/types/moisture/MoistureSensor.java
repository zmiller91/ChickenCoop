package coop.device.types.moisture;

import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.protocol.parser.EventParser;
import coop.device.Sensor;

public class MoistureSensor implements Device, Sensor {
    @Override
    public String getDescription() {
        return "Moisture Level Monitor";
    }

    @Override
    public EventParser getEventParser() {
        return new MoistureSensorParser();
    }

    @Override
    public ConfigKey[] getConfig() {
        return new ConfigKey[]{};
    }
}
