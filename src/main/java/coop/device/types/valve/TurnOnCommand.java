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
        DownlinkFrame command = new DownlinkFrame(serialNumber, opcode, params.get("zone"), params.get("duration"));
        command.setRequiresAck(true);
        command.setRequiresResources(true);
        return command;
    }

    @Override
    public boolean isValid(Map<String, String> params) {
        if(!params.containsKey("zone")) {
            return false;
        }

        if(!params.containsKey("duration")) {
            return false;
        }

        if(!NumberUtils.isParsable(params.get("duration"))) {
            return false;
        }


        return Long.parseLong(params.get("duration")) >= 0;
    }

    /**
     * Checks wether the provided frame represents this command.
     *
     * @param frame
     * @return
     */
    public boolean isCommand(DownlinkFrame frame) {
        return frame != null
                && frame.getPayload() != null
                && frame.getPayload().length == 3
                && frame.getPayload()[0].equals(opcode);
    }

    /**
     * Provides the target for this command. In this case it is the serial number of the device plus the zone. So
     * something like A8125.6.
     *
     * @param frame
     * @return
     */
    @Override
    public String getTarget(DownlinkFrame frame) {
        if(!isCommand(frame)) {
            return null;
        }

        return frame.getPayload()[0] + "." + frame.getPayload()[1];
    }

}
