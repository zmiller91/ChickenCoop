package coop.device.types.valve;

import coop.device.Action;
import coop.device.protocol.command.Command;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ValveAction {

    TURN_ON(new TurnOnCommand("1007"), "duration"),
    TURN_OFF(new TurnOnCommand("1007"));

    private final Action action;
    private final Command command;
    ValveAction(Command command, String... params) {
        this.action = new Action(this.name(), params);
        this.command = command;
    }

    public static ValveAction findByName(String name) {
        return Arrays.stream(ValveAction.values()).filter(v -> v.name().equals(name)).findFirst().orElse(null);
    }

}
