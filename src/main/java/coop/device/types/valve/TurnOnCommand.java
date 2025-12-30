package coop.device.types.valve;

import coop.device.protocol.command.Command;
import coop.device.protocol.DownlinkFrame;
import lombok.Data;

@Data
public class TurnOnCommand implements Command {

    public static final String COMMAND_ID = "1007";
    private long durationInMinutes;

    @Override
    public DownlinkFrame getCommand(String serialNumber) {
        DownlinkFrame command = new DownlinkFrame(serialNumber, COMMAND_ID, String.valueOf(durationInMinutes));
        command.setRequiresAck(true);
        return command;
    }

    @Override
    public boolean isValid() {
        return durationInMinutes >= 0;
    }
}
