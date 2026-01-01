package coop.shared.pi;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.component.ComponentConfig;
import coop.shared.database.table.Coop;
import coop.shared.database.table.rule.ComponentRuleTrigger;
import coop.shared.database.table.rule.RuleAction;
import coop.shared.database.table.rule.ScheduledRuleTrigger;
import coop.shared.pi.config.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StateFactory {

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private RuleRepository ruleRepository;

    public CoopState forCoop(Coop coop) {

        if(coop == null) {
            return new CoopState();
        }

        List<ComponentState> components = componentRepository
                .findByCoop(coop)
                .stream()
                .map( component -> {

                    Map<String, String> config = new HashMap<>();
                    for (ComponentConfig c : component.getConfig()) {
                        config.put(c.getKey(), c.getValue());
                    }

                    ComponentState state = new ComponentState();
                    state.setComponentId(component.getComponentId());
                    state.setConfig(config);
                    state.setDeviceType(component.getSerial().getDeviceType());
                    state.setSerialNumber(component.getSerial().getSerialNumber());
                    return state;

                }).toList();

        List<RuleState> rules = ruleRepository.findByCoop(coop)
                .stream()
                .map(rule -> {

                    RuleState state = new RuleState();
                    state.setRuleId(rule.getId());
                    state.setActions(toActionState(rule.getActions()));
                    state.setComponentTriggers(toComponentTriggerState(rule.getComponentTriggers()));
                    state.setScheduleTriggers(toScheduleTriggerState(rule.getScheduleTriggers()));
                    return state;

                }).toList();

        //TODO: Add group transforms here

        CoopState config = new CoopState();
        config.setCoopId(coop.getId());
        config.setComponents(components);
        config.setRules(rules);
        config.setAwsIotThingId(coop.getPi().getAwsIotThingId());
        return config;

    }

    private List<RuleScheduleTriggerState> toScheduleTriggerState(List<ScheduledRuleTrigger> triggers) {
        return triggers.stream().map(trigger -> {

            RuleScheduleTriggerState state = new RuleScheduleTriggerState();
            state.setId(trigger.getId());
            state.setFrequency(trigger.getFrequency().name());
            state.setGap(trigger.getGap());
            state.setHour(trigger.getHour());
            state.setMinute(trigger.getMinute());
            return state;

        }).toList();
    }

    private List<RuleComponentTriggerState> toComponentTriggerState(List<ComponentRuleTrigger> triggers) {
        return triggers.stream().map(trigger -> {

            RuleComponentTriggerState state = new RuleComponentTriggerState();
            state.setId(trigger.getId());
            state.setMetric(trigger.getMetric());
            state.setOperator(trigger.getOperator().name());
            state.setThreshold(trigger.getThreshold());
            state.setComponentId(trigger.getComponent().getComponentId());
            return state;

        }).toList();
    }

    private List<RuleActionState> toActionState(List<RuleAction> actions) {
        return actions.stream().map(action -> {
            RuleActionState state = new RuleActionState();
            state.setActionKey(action.getActionKey());
            state.setParams(action.getParamsMap());
            state.setId(action.getId());
            state.setComponentId(action.getComponent().getComponentId());
            return state;
        }).toList();
    }

}
