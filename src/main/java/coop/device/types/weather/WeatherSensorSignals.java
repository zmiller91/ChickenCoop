package coop.device.types.weather;

import coop.device.RuleSignal;
import lombok.Getter;

@Getter
public enum WeatherSensorSignals {

    TEMPERATURE,
    HUMIDITY;

    private final RuleSignal signal;
    WeatherSensorSignals() {
        this.signal = new RuleSignal(this.name());
    }
}