package coop.local;

import coop.device.Actuator;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.RxOpenEvent;
import coop.local.database.metric.MetricCacheEntry;
import coop.local.database.metric.MetricCacheRepository;
import coop.local.listener.EventListener;
import coop.local.scheduler.Scheduler;
import coop.local.state.LocalStateProvider;
import coop.shared.database.table.rule.Operator;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.RuleState;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class RuleProcessor implements EventListener {

    private final MetricCacheRepository metricCache;
    private final Scheduler scheduler;
    private final LocalStateProvider provider;

    public RuleProcessor(MetricCacheRepository metricCache, Scheduler scheduler, LocalStateProvider provider) {
        this.metricCache = metricCache;
        this.scheduler = scheduler;
        this.provider = provider;
    }

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return List.of(RxOpenEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {
        if(payload.getEvent() instanceof RxOpenEvent) {
            processRules(payload);
        }
    }

    private void processRules(EventPayload payload) {

        CoopState coop = payload.getCoop();
        ComponentState component = payload.getComponent();
        if(coop == null || coop.getRules() == null || component == null) {
            return;
        }

        // Only actuators can have actionKey sent to them
        if(!(component.getDeviceType().getDevice() instanceof Actuator device)) {
            return;
        }

        // Find all rules that have been satisfied
        List<RuleState> satisfiedRules = coop.getRules().
                stream()
                .filter(this::isRuleSatisfied)
                .toList();

        // Find all actions from those satisfied rules
        // Filter to only actions related to the component requesting a actionKey
        // Create the commands
        satisfiedRules
                .stream()
                .flatMap(rule -> rule.getActions().stream())
                .filter(action -> action.getComponentId().equals(component.getComponentId()))
                .filter(action -> device.validateCommand(action.getActionKey(), action.getParams()))
                .forEach(action -> {
                    DownlinkFrame downlink = device.createCommand(component.getSerialNumber(), action.getActionKey(), action.getParams());
                    scheduler.create(component, downlink, action.getId());
                });

        // Send a notification that the rule was executed
        //TODO: Should this go somewhere else?
        satisfiedRules.stream()
                .map(rule -> {
                    RuleSatisfiedHubEvent event = new RuleSatisfiedHubEvent();
                    event.setDt(System.currentTimeMillis());
                    event.setCoopId(coop.getCoopId());
                    event.setRuleId(rule.getRuleId());
                    event.setContext(context(rule));
                    return event;
                }).forEach(provider::save);
    }

    private boolean isRuleSatisfied(RuleState rule) {
        //TODO: Hard coding age limit to two hours should probably be configurable
        return rule.getComponentTriggers().stream().allMatch(trigger -> {
            MetricCacheEntry entry = metricCache.findRecent(trigger.getComponentId(), trigger.getMetric(), Duration.ofHours(2));
            return entry != null &&
                    entry.getValue() != null &&
                    Operator.valueOf(trigger.getOperator())
                            .evaluate(entry.getValue(), trigger.getThreshold());
        });
    }

    private Map<String, String> context(RuleState rule) {

        Map<String, String> context = new HashMap<>();
        rule.getComponentTriggers().forEach(trigger -> {
            MetricCacheEntry entry = metricCache.findRecent(trigger.getComponentId(), trigger.getMetric(), Duration.ofHours(2));
            String component = trigger.getComponentId();
            String value = String.valueOf(entry.getValue());
            context.put(component, value);
        });

        return context;
    }

}
