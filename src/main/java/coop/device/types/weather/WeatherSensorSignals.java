package coop.device.types.weather;

import coop.device.RuleSignal;
import lombok.Getter;

@Getter
public enum WeatherSensorSignals {

    TEMPERATURE("Temperature", "Measures the ambient temperature at the sensor location."),
    HUMIDITY("Humidity", "Measures the relative humidity of the air at the sensor location.");

    private final RuleSignal signal;
    WeatherSensorSignals(String displayName, String description) {
        this.signal = new RuleSignal(this.name(), displayName, description);
    }
}