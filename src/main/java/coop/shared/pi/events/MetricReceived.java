package coop.shared.pi.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MetricReceived extends HubEvent {
    private String componentId;
    private String metric;
    private Double value;

    @Override
    public HubEventType getType() {
        return HubEventType.METRIC;
    }
}
