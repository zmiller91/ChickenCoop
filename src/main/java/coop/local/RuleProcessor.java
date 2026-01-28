package coop.local;

import coop.device.Actuator;
import coop.device.Device;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.MetricEvent;
import coop.device.protocol.event.RxOpenEvent;
import coop.local.database.metric.MetricCacheEntry;
import coop.local.database.metric.MetricCacheRepository;
import coop.local.database.rule.RuleTriggerState;
import coop.local.database.rule.RuleTriggerStateRepository;
import coop.local.listener.EventListener;
import coop.local.scheduler.Scheduler;
import coop.local.state.LocalStateProvider;
import coop.shared.database.table.rule.Operator;
import coop.shared.pi.config.*;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static coop.local.database.rule.TriggerState.ACTIVE;
import static coop.local.database.rule.TriggerState.INACTIVE;

@Log4j2
public class RuleProcessor implements EventListener {

    private final MetricCacheRepository metricCache;
    private final RuleTriggerStateRepository triggerStateRepository;
    private final Scheduler scheduler;
    private final LocalStateProvider provider;

    public RuleProcessor(MetricCacheRepository metricCache,
                         RuleTriggerStateRepository triggerStateRepository,
                         Scheduler scheduler,
                         LocalStateProvider provider) {
        this.metricCache = metricCache;
        this.triggerStateRepository = triggerStateRepository;
        this.scheduler = scheduler;
        this.provider = provider;
    }

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return List.of(MetricEvent.class, RxOpenEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {
        processRules(payload);
    }

    /**
     * Rules are processed from metric events and all actions are pre-created with the scheduler. This ensures that all
     * events happen exactly once (messages, actions, etc) because we can block subsequent triggers. Right now there is
     * a hard-coded 24 hour expiry.
     *
     * @param payload
     */
    private void processRules(EventPayload payload) {

        CoopState coop = payload.getCoop();
        ComponentState component = payload.getComponent();
        if(coop == null || coop.getRules() == null || component == null) {
            return;
        }

        // Create a quick ComponentState lookup for efficient querying
        Map<String, ComponentState> componentsById =
                Optional.ofNullable(coop.getComponents())
                        .orElseGet(List::of)
                        .stream()
                        .collect(Collectors.toMap(
                                ComponentState::getComponentId,
                                Function.identity(),
                                (a, b) -> a
                        ));


        // Only look at rules that use the component in a trigger or action
        List<RuleState> applicableRules = coop.getRules()
                .stream()
                .filter(rule -> {

                    boolean isTriggerComponent = rule.getComponentTriggers() != null &&
                            rule.getComponentTriggers()
                                    .stream()
                                    .map(RuleComponentTriggerState::getComponentId)
                                    .anyMatch(id -> id.equals(component.getComponentId()));

                    boolean isActionComponent = rule.getActions() != null &&
                            rule.getActions()
                                    .stream()
                                    .map(RuleActionState::getComponentId)
                                    .anyMatch(id -> id.equals(component.getComponentId()));

                    return isTriggerComponent || isActionComponent;
                })
                .toList();

        for(RuleState rule : applicableRules) {
            if(isRuleSatisfied(rule)) {
                executeRule(coop, rule, componentsById);
            } else {
                markInactive(rule);
            }
        }
    }

    private void markInactive(RuleState rule) {
        RuleTriggerState triggerState = triggerStateRepository.findByRuleId(rule.getRuleId());
        if(triggerState != null && ACTIVE.equals(triggerState.getTriggerState())) {
            triggerState.setTriggerState(INACTIVE);
            triggerState.setTriggerStateDt(System.currentTimeMillis());
            triggerStateRepository.persist(triggerState);
        }
    }

    private void executeRule(CoopState coop, RuleState rule, Map<String, ComponentState> componentsById) {

        long now = System.currentTimeMillis();

        // Check the status of the trigger. If it's already active, then don't re-activate it unless it has been
        // more than 24 hours.
        // TODO: We should probably make the timeout configurable
        RuleTriggerState triggerState = triggerStateRepository.findByRuleId(rule.getRuleId());
        if(triggerState != null &&
                ACTIVE.equals(triggerState.getTriggerState()) &&
                now - triggerState.getTriggerStateDt() < Duration.ofHours(24).toMillis()) {
            return;
        }

        // Pre-create all the actions. When the component tied to the action sends an RX Open event, the scheduler
        // will pick it up and start the job.
        // TODO: If an action can't be created, then should we bubble up a failure?
        for(RuleActionState action : Optional.ofNullable(rule.getActions()).orElseGet(List::of)) {

            ComponentState actionComponent = componentsById.get(action.getComponentId());
            if(actionComponent == null) {
                continue;
            }

            // Only actuators can have a job sent to them. So ensure it's an actuator and ensure the action is valid
            // before sending to the scheduler.
            Device device = actionComponent.getDeviceType().getDevice();
            if(device instanceof Actuator actuator) {
                boolean isCommandValid = actuator.validateCommand(action.getActionKey(), action.getParams());
                if(isCommandValid) {
                    DownlinkFrame downlink = actuator.createCommand(actionComponent.getSerialNumber(), action.getActionKey(), action.getParams());
                    scheduler.create(actionComponent, downlink, action.getId());
                }
            }
        }

        // Set the rule trigger state to active
        triggerState = ObjectUtils.firstNonNull(triggerState, new RuleTriggerState());
        triggerState.setRuleId(rule.getRuleId());
        triggerState.setTriggerState(ACTIVE);
        triggerState.setTriggerStateDt(now);
        triggerStateRepository.persist(triggerState);

        // Emit an event to notify the user that the rule was executed
        RuleSatisfiedHubEvent event = new RuleSatisfiedHubEvent();
        event.setDt(now);
        event.setCoopId(coop.getCoopId());
        event.setRuleId(rule.getRuleId());
        event.setContext(context(rule));
        provider.save(event);
    }

    //TODO: If the operator is invalid, then should we return false or bubble up a failure?
    private boolean isRuleSatisfied(RuleState rule) {
        //TODO: Hard coding age limit to two hours should probably be configurable
        return rule.getComponentTriggers().stream().allMatch(trigger -> {
            try {

                MetricCacheEntry entry = metricCache.findRecent(trigger.getComponentId(), trigger.getMetric(), Duration.ofHours(2));
                return entry != null &&
                        entry.getValue() != null &&
                        Operator.valueOf(trigger.getOperator())
                                .evaluate(entry.getValue(), trigger.getThreshold());

            } catch (IllegalStateException | NullPointerException e) {
                // Thrown when the operator doesn't exist
                return false;
            }
        });
    }

    private Map<String, String> context(RuleState rule) {

        Map<String, String> context = new HashMap<>();
        rule.getComponentTriggers().forEach(trigger -> {
            MetricCacheEntry entry = metricCache.findRecent(trigger.getComponentId(), trigger.getMetric(), Duration.ofHours(2));
            if(entry != null && entry.getValue() != null) {
                String component = trigger.getComponentId();
                String value = String.valueOf(entry.getValue());
                context.put(component, value);
            }
        });

        return context;
    }

}
