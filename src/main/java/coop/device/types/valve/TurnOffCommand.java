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
        DownlinkFrame command = new DownlinkFrame(serialNumber, opcode, params.get("zone"));
        command.setRequiresAck(true);
        command.setRequiresResources(false);
        return command;
    }

    @Override
    public boolean isValid(Map<String, String> params) {
        if(!params.containsKey("zone")) {
            return false;
        }

        return true;
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
                && frame.getPayload().length == 2
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
