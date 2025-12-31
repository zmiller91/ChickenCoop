package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

@Data
public class RxOpenEvent implements Event {

    public static final String TAG = "RXOPEN";

    private String serialNumber;
    private long duration;

    public static RxOpenEvent from(UplinkFrame frame) {
        RxOpenEvent rxopen = new RxOpenEvent();
        rxopen.setSerialNumber(frame.getSerialNumber());
        rxopen.setDuration(frame.getLongAt(1));
        return rxopen;
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2) &&
                TAG.equals(frame.getStringAt(0));
    }
}
