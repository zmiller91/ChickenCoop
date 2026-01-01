package coop.device.types.valve;

import coop.device.protocol.command.Command;
import coop.device.protocol.DownlinkFrame;
import lombok.Data;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;

@Data
public class TurnOnCommand implements Command {

    private final String opcode;

    public TurnOnCommand(String opcode) {
        this.opcode = opcode;
    }

    @Override
    public DownlinkFrame getCommand(String serialNumber, Map<String, String> params) {
        DownlinkFrame command = new DownlinkFrame(serialNumber, opcode, String.valueOf(params.get("duration")));
        command.setRequiresAck(true);
        return command;
    }

    @Override
    public boolean isValid(Map<String, String> params) {
        if(!params.containsKey("duration")) {
            return false;
        }

        if(!NumberUtils.isParsable(params.get("duration"))) {
            return false;
        }


        return Long.parseLong(params.get("duration")) >= 0;
    }
}
