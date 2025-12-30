package coop.device.types.weather;

import coop.device.protocol.event.Event;
import coop.device.protocol.parser.EventParser;
import coop.device.protocol.event.MetricEvent;
import coop.device.protocol.UplinkFrame;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.math.NumberUtils.toDouble;

public class WeatherSensorMessageParser implements EventParser {

    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(!frame.isValid(2)) {
            return null;
        }

        Double temperature = frame.getDouble(0);
        Double humidity = frame.getDouble(1);

        List<Event> events = new ArrayList<>();
        if(temperature != null) {
            events.add(new MetricEvent(frame.getSerialNumber(), "TEMPERATURE", temperature / 100.0));
        }

        if(humidity != null) {
            events.add(new MetricEvent(frame.getSerialNumber(), "HUMIDITY", humidity / 1024));
        }

        return events;
    }
}
