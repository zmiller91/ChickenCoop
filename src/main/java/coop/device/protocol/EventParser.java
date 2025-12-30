package coop.device.protocol;

import java.util.List;

public interface EventParser {

    List<Event> parse(UplinkFrame frame);


}
