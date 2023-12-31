package coop.shared.pi.metric;

import lombok.Data;

@Data
public class Metric {
    private String clientId;
    private long dt;
    private String coopId;
    private String componentId;
    private String metric;
    private Double value;
}
