package coop.device.types.scale;

import coop.device.RuleSignal;
import lombok.Getter;

@Getter
public enum ScaleSensorSignals {

    WEIGHT;

    private final RuleSignal signal;
    ScaleSensorSignals() {
        this.signal = new RuleSignal(this.name());
    }

}
