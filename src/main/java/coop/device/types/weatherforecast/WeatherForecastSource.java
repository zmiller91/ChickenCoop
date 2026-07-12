package coop.device.types.weatherforecast;

import coop.device.*;
import coop.device.protocol.parser.EventParser;

import java.util.List;

import static coop.device.types.weatherforecast.WeatherForecastSignals.RAIN_AMOUNT_24H;
import static coop.device.types.weatherforecast.WeatherForecastSignals.RAIN_PROBABILITY_24H;

/**
 * A virtual device, not a physical one - it never receives real serial uplinks. WeatherForecastFetcher
 * periodically fetches an external forecast for this component's configured latitude/longitude and reports it
 * as ordinary metric events, exactly like a real sensor would, so it flows through the same rule/metric
 * pipeline as everything else. Not to be confused with coop.device.types.weather.WeatherSensor, a real local
 * humidity/temperature sensor.
 */
public class WeatherForecastSource implements Sensor, Device, RuleSource {

    public static final ConfigKey LATITUDE = new ConfigKey("latitude", "Latitude");
    public static final ConfigKey LONGITUDE = new ConfigKey("longitude", "Longitude");

    @Override
    public String getDescription() {
        return "Weather Forecast";
    }

    @Override
    public EventParser getEventParser() {
        // Never actually invoked - EventProcessor.receiveMessage() only calls this after matching a real
        // uplink frame's serial number to a component, which never happens for this virtual device.
        return frame -> List.of();
    }

    @Override
    public ConfigKey[] getConfig() {
        return new ConfigKey[]{LATITUDE, LONGITUDE};
    }

    @Override
    public List<RuleSignal> getRuleMetrics() {
        return List.of(RAIN_PROBABILITY_24H.getSignal(), RAIN_AMOUNT_24H.getSignal());
    }
}
