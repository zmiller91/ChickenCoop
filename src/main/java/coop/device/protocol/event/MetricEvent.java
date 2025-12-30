package coop.device.protocol.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MetricEvent implements Event {

    private final String serialNumber;
    private final String metric;
    private final double value;

}
