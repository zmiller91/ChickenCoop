package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

@Data
public class ManualOverrideEvent implements Event {

    public static final String TAG = "LCLOVR";

    private String serialNumber;
    private String payload;

    public static ManualOverrideEvent from(UplinkFrame frame) {
        ManualOverrideEvent lclovr = new ManualOverrideEvent();
        lclovr.setSerialNumber(frame.getSerialNumber());
        lclovr.setPayload(frame.getPayloadFromIdx(1));
        return lclovr;
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2, true) &&
                TAG.equals(frame.getStringAt(0));
    }
}
