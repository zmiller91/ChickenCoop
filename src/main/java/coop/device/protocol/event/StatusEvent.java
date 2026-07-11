package coop.device.protocol.event;

import coop.device.protocol.UplinkFrame;
import lombok.Data;

/**
 * A generic device-reported status broadcast, independent of any specific command's lifecycle. Wire shape is
 * STATUS::<type>::<arbitrary sequence>, where type says what kind of status this is (e.g. TURN_ON/TURN_OFF
 * for a port's actual on/off state, or something unrelated like BATTERY_PCT) and the rest of the payload is
 * type-specific - interpreted by whichever device/listener cares about that type.
 */
@Data
public class StatusEvent implements Event {

    public static final String TAG = "STATUS";

    private String serialNumber;
    private String type;
    private String payload;

    public static StatusEvent from(UplinkFrame frame) {
        StatusEvent event = new StatusEvent();
        event.setSerialNumber(frame.getSerialNumber());
        event.setType(frame.getStringAt(1));
        event.setPayload(frame.getPayloadFromIdx(2));
        return event;
    }

    public static boolean isEvent(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2, true) &&
                TAG.equals(frame.getStringAt(0));
    }
}
