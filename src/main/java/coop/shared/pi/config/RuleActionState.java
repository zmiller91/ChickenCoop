package coop.shared.pi.config;

import lombok.Data;
import java.util.Map;

@Data
public class RuleActionState {
    private String id;
    private String componentId;
    private String actionKey;
    private Map<String, String> params;
}