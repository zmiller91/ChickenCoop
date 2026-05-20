package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

@Data
public class ManualRequestEvent implements Event {

    public static final String TAG = "LCLREQ";

    private String serialNumber;
    private String payload;

    public static ManualRequestEvent from(UplinkFrame frame) {
        ManualRequestEvent lclreq = new ManualRequestEvent();
        lclreq.setSerialNumber(frame.getSerialNumber());
        lclreq.setPayload(frame.getStringAt(1));
        return lclreq;
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2) &&
                TAG.equals(frame.getStringAt(0));
    }
}
