package coop.device.types.scale;

import coop.device.protocol.event.Event;
import coop.device.protocol.parser.EventParser;
import coop.device.protocol.event.MetricEvent;
import coop.device.protocol.UplinkFrame;

import java.util.List;

import static coop.device.types.scale.ScaleSensorSignals.WEIGHT;

public class ScaleMessageParser implements EventParser {

    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(frame.isValid(1)) {
            Double weight = frame.getDoubleAt(0);
            if(weight != null) {
                return List.of(new MetricEvent(frame.getSerialNumber(), WEIGHT.getSignal().getKey(), weight));
            }
        }

        return List.of();
    }
}
