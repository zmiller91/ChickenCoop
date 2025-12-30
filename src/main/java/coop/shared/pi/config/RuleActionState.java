package coop.shared.pi.config;

import lombok.Data;

@Data
public class RuleActionState {
    private String id;
    private String componentId;
    private String action;
}
