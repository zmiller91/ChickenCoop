package coop.shared.pi.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleSatisfiedHubEvent extends HubEvent {
    private String ruleId;
    private Map<String, String> context;
}
