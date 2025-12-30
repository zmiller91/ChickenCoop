package coop.device.valve;

import coop.device.protocol.*;

import java.util.Collections;
import java.util.List;

public class ValveEventParser implements EventParser {
    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(isCommandRequest(frame)) {
            return Collections.singletonList(new CommandRequestEvent(frame.getSerialNumber()));
        }

        if(isAck(frame)) {
            AckEvent ack = new AckEvent();
            ack.setSerialNumber(frame.getSerialNumber());
            ack.setMessageId(frame.getStringAt(1));
            return Collections.singletonList(ack);
        }

        return List.of();
    }

    private boolean isAck(UplinkFrame frame) {
        return frame != null &&
                frame.isValid(2) &&
                "ACK".equals(frame.getStringAt(0));
    }

    private boolean isCommandRequest(UplinkFrame frame) {
        return frame != null
                && frame.isValid(1)
                && "CMDREQ".equals(frame.getStringAt(0));
    }
}
