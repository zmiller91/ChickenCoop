package coop.device.protocol.parser;

import coop.device.protocol.*;
import coop.device.protocol.event.AckEvent;
import coop.device.protocol.event.CommandCompleteEvent;
import coop.device.protocol.event.CommandRequestEvent;
import coop.device.protocol.event.Event;

import java.util.Collections;
import java.util.List;

public class CommandEventParser implements EventParser {
    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(CommandRequestEvent.isEvent(frame)) {
            return Collections.singletonList(CommandRequestEvent.from(frame));
        }

        if(AckEvent.isEvent(frame)) {
            return Collections.singletonList(AckEvent.from(frame));
        }

        if(CommandCompleteEvent.isEvent(frame)) {
            return Collections.singletonList(CommandCompleteEvent.from(frame));
        }

        return List.of();
    }
}
