package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

@Data
public class CommandCompleteEvent implements Event {

    public static final String TAG = "CMDCMPLT";

    private String serialNumber;
    private String messageId;

    public static CommandCompleteEvent from(UplinkFrame frame) {
        CommandCompleteEvent event = new CommandCompleteEvent();
        event.setSerialNumber(frame.getSerialNumber());
        event.setMessageId(frame.getStringAt(1));
        return event;
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2) &&
                TAG.equals(frame.getStringAt(0));
    }
}
