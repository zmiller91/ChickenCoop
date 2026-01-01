package coop.device.types.valve;

import coop.device.Action;
import coop.device.Actuator;
import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.protocol.DownlinkFrame;
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
        return new ConfigKey[0];
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
}
