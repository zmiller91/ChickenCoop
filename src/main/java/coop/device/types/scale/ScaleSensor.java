package coop.device.types.scale;

import coop.device.*;
import coop.device.protocol.parser.EventParser;

import java.util.List;

import static coop.device.types.scale.ScaleSensorSignals.WEIGHT;

public class ScaleSensor implements Device, Sensor, RuleSource {
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

    @Override
    public List<RuleSignal> getRuleMetrics() {
        return List.of(WEIGHT.getSignal());
    }
}
