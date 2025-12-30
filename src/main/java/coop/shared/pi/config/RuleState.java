package coop.shared.pi.config;

import lombok.Data;

import java.util.List;

@Data
public class RuleState {
    private String ruleId;
    private List<RuleComponentTriggerState> componentTriggers;
    private List<RuleScheduleTriggerState> scheduleTriggers;
    private List<RuleActionState> actions;
}
