package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

@Data
public class CommandRequestEvent implements Event {

    public static final String TAG = "CMDREQ";

    private final String serialNumber;

    public static CommandRequestEvent from(UplinkFrame frame) {
        return new CommandRequestEvent(frame.getSerialNumber());
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null
                && frame.isValid(1)
                && TAG.equals(frame.getStringAt(0));
    }
}
