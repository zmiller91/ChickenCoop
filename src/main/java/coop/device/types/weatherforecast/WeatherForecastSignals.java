package coop.device.types.weatherforecast;

import coop.device.RuleSignal;
import lombok.Getter;

/**
 * Not to be confused with coop.device.types.weather.WeatherSensorSignals, which is a real local
 * humidity/temperature sensor - these are external forecast values fetched by WeatherForecastFetcher, not
 * anything reported by physical hardware over serial.
 *
 * Two different shapes of signal, dispatched differently by WeatherForecastFetcher:
 *  - RAIN_PROBABILITY_24H/RAIN_AMOUNT_24H are forward-looking windowed aggregates (max/sum over the *next*
 *    24h, from Open-Meteo's forecast) - built for gating a rule, e.g. "skip today's watering if enough rain
 *    is forecast." RAIN_ACTUAL_24H is the backward-looking counterpart (sum over the *past* 24h, from
 *    Open-Meteo's past_days data) - answers "did it actually rain," e.g. "skip watering if it already rained
 *    enough," which a forecast alone can't tell you after the fact.
 *  - Everything else is a point-in-time snapshot (the nearest hour's value) - built for building a historical
 *    time series to feed to future analysis, not for gating anything over a window.
 */
@Getter
public enum WeatherForecastSignals {

    RAIN_PROBABILITY_24H,
    RAIN_AMOUNT_24H,
    RAIN_ACTUAL_24H,

    TEMPERATURE,
    HUMIDITY,
    WIND_SPEED,
    CLOUD_COVER,
    EVAPOTRANSPIRATION,
    DEW_POINT,
    UV_INDEX,
    SOLAR_RADIATION;

    private final RuleSignal signal;
    WeatherForecastSignals() {
        this.signal = new RuleSignal(this.name());
    }
}
