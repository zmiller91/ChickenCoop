package coop.device.types.moisture;

import coop.device.RuleSignal;
import lombok.Getter;

@Getter
public enum MoistureSensorSignals {

    MOISTURE_PERCENT;

    private final RuleSignal signal;
    MoistureSensorSignals() {
        this.signal = new RuleSignal(this.name());
    }

}
