package coop.device.weather;

import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.protocol.EventParser;
import coop.device.Sensor;

public class WeatherSensor implements Sensor, Device {

    @Override
    public String getDescription() {
        return "Weather Sensor";
    }

    @Override
    public EventParser getEventParser() {
        return new WeatherSensorMessageParser();
    }

    @Override
    public ConfigKey[] getConfig() {
        return new ConfigKey[]{};
    }

}
