package coop.device.types.valve;

import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.command.Command;

import java.util.Map;

public class TurnOffCommand implements Command {

    private final String opcode;

    public TurnOffCommand(String opcode) {
        this.opcode = opcode;
    }

    @Override
    public DownlinkFrame getCommand(String serialNumber, Map<String, String> params) {
        DownlinkFrame command = new DownlinkFrame(serialNumber, opcode);
        command.setRequiresAck(true);
        command.setRequiresResources(false);
        return command;
    }

    @Override
    public boolean isValid(Map<String, String> params) {
        return true;
    }
}
