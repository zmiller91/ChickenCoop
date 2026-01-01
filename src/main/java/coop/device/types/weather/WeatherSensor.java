package coop.device.types.weather;

import coop.device.*;
import coop.device.protocol.parser.EventParser;

import java.util.List;

import static coop.device.types.weather.WeatherSensorSignals.HUMIDITY;
import static coop.device.types.weather.WeatherSensorSignals.TEMPERATURE;

public class WeatherSensor implements Sensor, Device, RuleSource {

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

    @Override
    public List<RuleSignal> getRuleMetrics() {
        return List.of(TEMPERATURE.getSignal(), HUMIDITY.getSignal());
    }
}
