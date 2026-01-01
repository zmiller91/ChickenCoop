package coop.device.types.scale;

import coop.device.RuleSignal;
import lombok.Getter;

@Getter
public enum ScaleSensorSignals {

    WEIGHT("Weight", "Measures the total weight currently detected by the scale.");

    private final RuleSignal signal;
    ScaleSensorSignals(String displayName, String description) {
        this.signal = new RuleSignal(this.name(), displayName, description);
    }

}
