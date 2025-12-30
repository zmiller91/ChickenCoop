package coop.device.types.valve;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import coop.device.Actuator;
import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.protocol.command.Command;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.parser.CommandEventParser;
import coop.device.protocol.parser.EventParser;

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
    public boolean validateCommand(JsonObject object) {
        Command command = getCommand(object);
        return command != null && command.isValid();
    }

    @Override
    public DownlinkFrame createCommand(String serialNumber, JsonObject object) {
        Command command = getCommand(object);
        if(command == null) {
            return null;
        }

        return command.getCommand(serialNumber);
    }

    private Command getCommand(JsonObject object) {

        if(object == null) {
            return null;
        }

        try {
            String commandId = object.get("commandId").getAsString();
            if (commandId.equals(TurnOnCommand.COMMAND_ID)) {
                return new Gson().fromJson(object, TurnOnCommand.class);
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }
}
