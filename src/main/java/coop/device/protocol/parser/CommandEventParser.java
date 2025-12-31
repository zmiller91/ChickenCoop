package coop.device.protocol.parser;

import coop.device.protocol.*;
import coop.device.protocol.event.*;

import java.util.Collections;
import java.util.List;

public class CommandEventParser implements EventParser {
    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(AckEvent.isEvent(frame)) {
            return Collections.singletonList(AckEvent.from(frame));
        }

        if(CommandCompleteEvent.isEvent(frame)) {
            return Collections.singletonList(CommandCompleteEvent.from(frame));
        }

        if(RxOpenEvent.isEvent(frame)) {
            return Collections.singletonList(RxOpenEvent.from(frame));
        }

        return List.of();
    }
}
