package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

@Data
public class AckEvent implements Event {

    public static final String TAG = "ACK";

    private String serialNumber;
    private String messageId;

    public static AckEvent from(UplinkFrame frame) {
        AckEvent ack = new AckEvent();
        ack.setSerialNumber(frame.getSerialNumber());
        ack.setMessageId(frame.getStringAt(1));
        return ack;
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2) &&
                TAG.equals(frame.getStringAt(0));
    }
}
