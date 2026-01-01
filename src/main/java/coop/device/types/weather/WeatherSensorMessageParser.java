package coop.device.types.weather;

import coop.device.protocol.event.Event;
import coop.device.protocol.parser.EventParser;
import coop.device.protocol.event.MetricEvent;
import coop.device.protocol.UplinkFrame;

import java.util.ArrayList;
import java.util.List;

import static coop.device.types.weather.WeatherSensorSignals.HUMIDITY;
import static coop.device.types.weather.WeatherSensorSignals.TEMPERATURE;

public class WeatherSensorMessageParser implements EventParser {

    @Override
    public List<Event> parse(UplinkFrame frame) {

        if(!frame.isValid(2)) {
            return null;
        }

        Double temperature = frame.getDoubleAt(0);
        Double humidity = frame.getDoubleAt(1);

        List<Event> events = new ArrayList<>();
        if(temperature != null) {
            events.add(new MetricEvent(frame.getSerialNumber(), TEMPERATURE.getSignal().getKey(), temperature / 100.0));
        }

        if(humidity != null) {
            events.add(new MetricEvent(frame.getSerialNumber(), HUMIDITY.getSignal().getKey(), humidity / 1024));
        }

        return events;
    }
}
