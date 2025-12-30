package coop.device.protocol.parser;

import coop.device.protocol.UplinkFrame;
import coop.device.protocol.event.Event;

import java.util.List;

public interface EventParser {

    List<Event> parse(UplinkFrame frame);


}
