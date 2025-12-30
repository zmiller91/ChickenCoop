package coop.shared.pi.config;

import lombok.Data;

import java.util.List;

@Data
public class CoopState {
    private String coopId;
    private String awsIotThingId;
    private List<ComponentState> components;
    private List<RuleState> rules;
}
