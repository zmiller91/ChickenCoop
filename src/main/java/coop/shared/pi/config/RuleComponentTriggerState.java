package coop.shared.pi.config;

import lombok.Data;

@Data
public class RuleComponentTriggerState {
    private String id;
    private String componentId;
    private String metric;
    private String operator;
    private double threshold;
}
