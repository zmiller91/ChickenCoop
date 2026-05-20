package coop.shared.pi.config;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class CoopState {
    private String coopId;
    private String awsIotThingId;
    private List<ComponentState> components;
    private List<RuleState> rules;
    private List<GroupResourceState> groupResources;
    private List<GlobalResourceState> globalResources;

    public Map<String, ComponentState> getComponentMap() {

        return getComponents().stream().collect(Collectors.toMap(
                ComponentState::getComponentId,
                c -> c
        ));
    }

}
