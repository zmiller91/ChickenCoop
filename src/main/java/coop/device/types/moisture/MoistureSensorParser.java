package coop.device.types.moisture;

import coop.device.protocol.event.Event;
import coop.device.protocol.parser.EventParser;
import coop.device.protocol.event.MetricEvent;
import coop.device.protocol.UplinkFrame;

import java.util.List;

import static coop.device.types.moisture.MoistureSensorSignals.MOISTURE_PERCENT;

public class MoistureSensorParser implements EventParser {

    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(frame.isValid(1)) {
            Double moisture = frame.getDoubleAt(0);
            if(moisture != null) {
                return List.of(new MetricEvent(frame.getSerialNumber(), MOISTURE_PERCENT.getSignal().getKey(), moisture / 100.0));
            }
        }

        return List.of();
    }
}
