package coop.device.types.moisture;

import coop.device.*;
import coop.device.protocol.parser.EventParser;

import java.util.List;

import static coop.device.types.moisture.MoistureSensorSignals.MOISTURE_PERCENT;

public class MoistureSensor implements Device, Sensor, RuleSource {
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

    @Override
    public List<RuleSignal> getRuleMetrics() {
        return List.of(MOISTURE_PERCENT.getSignal());
    }
}
