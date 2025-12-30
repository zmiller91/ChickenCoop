package coop.device.scale;

import coop.device.protocol.Event;
import coop.device.protocol.EventParser;
import coop.device.protocol.MetricEvent;
import coop.device.protocol.UplinkFrame;

import java.util.List;

public class ScaleMessageParser implements EventParser {

    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(frame.isValid(1)) {
            Double weight = frame.getDouble(0);
            if(weight != null) {
                return List.of(new MetricEvent(frame.getSerialNumber(), "WEIGHT", weight));
            }
        }

        return List.of();
    }
}
