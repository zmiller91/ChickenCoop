package coop.local;

import coop.device.Actuator;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.RxOpenEvent;
import coop.local.database.metric.MetricCacheEntry;
import coop.local.database.metric.MetricCacheRepository;
import coop.local.listener.EventListener;
import coop.local.scheduler.Scheduler;
import coop.shared.database.table.rule.Operator;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.RuleState;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.List;

@Log4j2
public class RuleProcessor implements EventListener {

    private final MetricCacheRepository metricCache;
    private final Scheduler scheduler;

    public RuleProcessor(MetricCacheRepository metricCache, Scheduler scheduler) {
        this.metricCache = metricCache;
        this.scheduler = scheduler;
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
        // Find all actions from those satisfied rules
        // Filter to only actions related to the component requesting a actionKey
        // Create the commands
        coop.getRules()
                .stream()
                .filter(this::isRuleSatisfied)
                .flatMap(rule -> rule.getActions().stream())
                .filter(action -> action.getComponentId().equals(component.getComponentId()))
                .filter(action -> device.validateCommand(action.getActionKey(), action.getParams()))
                .forEach(action -> {
                    DownlinkFrame downlink = device.createCommand(component.getSerialNumber(), action.getActionKey(), action.getParams());
                    scheduler.create(component, downlink, action.getId());
                });
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

}
