package coop.device.types.moisture;

import coop.device.RuleSignal;
import lombok.Getter;

@Getter
public enum MoistureSensorSignals {

    MOISTURE_PERCENT("Soil Moisture (%)", "Measures relative water content in soil.");

    private final RuleSignal signal;
    MoistureSensorSignals(String displayName, String description) {
        this.signal = new RuleSignal(this.name(), displayName, description);
    }

}
