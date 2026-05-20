package coop.device.types.valve;

import coop.device.Action;
import coop.device.Actuator;
import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.ManualOverrideEvent;
import coop.device.protocol.event.ManualRequestEvent;
import coop.device.protocol.parser.CommandEventParser;
import coop.device.protocol.parser.EventParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ValveActuator implements Device, Actuator {
    @Override
    public String getDescription() {
        return "Water Valve";
    }

    @Override
    public EventParser getEventParser() {
        return new CommandEventParser();
    }

    @Override
    public ConfigKey[] getConfig() {
        ConfigKey defaultDuration = new ConfigKey("default_duration", "Default Duration");
        ConfigKey manualCutoff = new ConfigKey("manual_cutoff", "Manual Cutoff");
        ConfigKey isAlwaysOn = new ConfigKey("always_on", "Always On");
        return new ConfigKey[]{defaultDuration, manualCutoff, isAlwaysOn};
    }

    @Override
    public boolean validateCommand(String name, Map<String, String> params) {
        ValveAction action = ValveAction.findByName(name);
        return action != null && action.getCommand().isValid(params);
    }

    @Override
    public DownlinkFrame createCommand(String serialNumber, String name, Map<String, String> params) {
        if(!validateCommand(name, params)) {
            return null;
        }
        ValveAction action = ValveAction.findByName(name);
        return action.getCommand().getCommand(serialNumber, params);
    }

    @Override
    public List<Action> getActions() {
        return Arrays.stream(ValveAction.values()).map(ValveAction::getAction).toList();
    }

    @Override
    public DownlinkFrame manualRequest(ManualRequestEvent event, String serialNumber, Map<String, String> componentConfig) {

        if(componentConfig == null || !componentConfig.containsKey("default_duration")) {
            return null;
        }

        if(!("ON".equals(event.getPayload()) || "OFF".equals(event.getPayload()))) {
            return null;
        }

        ValveAction action = "ON".equals(event.getPayload()) ? ValveAction.TURN_ON : ValveAction.TURN_OFF;

        return createCommand(serialNumber,
                action.name(),
                Map.of("duration", componentConfig.get("default_duration")));
    }

    @Override
    public DownlinkFrame manualOverride(ManualOverrideEvent event, String serialNumber, Map<String, String> componentConfig) {

        if(componentConfig == null || !componentConfig.containsKey("manual_cutoff")) {
            return null;
        }

        if(!("ON".equals(event.getPayload()) || "OFF".equals(event.getPayload()))) {
            return null;
        }

        ValveAction action = "ON".equals(event.getPayload()) ? ValveAction.TURN_ON : ValveAction.TURN_OFF;

        return createCommand(serialNumber,
                action.name(),
                Map.of("duration", componentConfig.get("manual_cutoff")));
    }
}
