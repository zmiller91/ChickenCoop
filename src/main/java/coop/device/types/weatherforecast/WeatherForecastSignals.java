package coop.device.types.weatherforecast;

import coop.device.RuleSignal;
import lombok.Getter;

/**
 * Not to be confused with coop.device.types.weather.WeatherSensorSignals, which is a real local
 * humidity/temperature sensor - these are external forecast values fetched by WeatherForecastFetcher, not
 * anything reported by physical hardware over serial.
 */
@Getter
public enum WeatherForecastSignals {

    RAIN_PROBABILITY_24H,
    RAIN_AMOUNT_24H;

    private final RuleSignal signal;
    WeatherForecastSignals() {
        this.signal = new RuleSignal(this.name());
    }
}
