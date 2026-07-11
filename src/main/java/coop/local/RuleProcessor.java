package coop.local;

import coop.device.Actuator;
import coop.device.Device;
import coop.device.PortCommand;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.MetricEvent;
import coop.device.protocol.event.RxOpenEvent;
import coop.local.database.metric.MetricCacheEntry;
import coop.local.database.metric.MetricCacheRepository;
import coop.local.database.rule.RuleTriggerState;
import coop.local.database.rule.RuleTriggerStateRepository;
import coop.local.database.rule.ScheduleTriggerState;
import coop.local.database.rule.ScheduleTriggerStateRepository;
import coop.local.listener.EventListener;
import coop.local.scheduler.Scheduler;
import coop.local.state.LocalStateProvider;
import coop.shared.database.table.rule.Operator;
import coop.shared.database.table.rule.ScheduleFrequency;
import coop.shared.pi.config.*;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.pi.events.PortActionHubEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static coop.local.database.rule.TriggerState.ACTIVE;
import static coop.local.database.rule.TriggerState.INACTIVE;

@Log4j2
public class RuleProcessor implements EventListener, Invokable {

    private final MetricCacheRepository metricCache;
    private final RuleTriggerStateRepository triggerStateRepository;
    private final ScheduleTriggerStateRepository scheduleTriggerStateRepository;
    private final Scheduler scheduler;
    private final LocalStateProvider provider;

    public RuleProcessor(MetricCacheRepository metricCache,
                         RuleTriggerStateRepository triggerStateRepository,
                         ScheduleTriggerStateRepository scheduleTriggerStateRepository,
                         Scheduler scheduler,
                         LocalStateProvider provider) {
        this.metricCache = metricCache;
        this.triggerStateRepository = triggerStateRepository;
        this.scheduleTriggerStateRepository = scheduleTriggerStateRepository;
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

            try {
                if (isRuleSatisfied(rule)) {
                    executeRule(coop, rule, componentsById);
                } else {
                    markInactive(rule);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                log.error("Failed to process rule " + rule.getRuleId(), t);
            }
        }
    }

    private void markInactive(RuleState rule) {
        RuleTriggerState triggerState = triggerStateRepository.findByRuleId(rule.getRuleId());
        if(triggerState != null && ACTIVE.equals(triggerState.getTriggerState())) {
            triggerStateRepository.upsert(rule.getRuleId(), INACTIVE);
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

        dispatchRuleActions(coop, rule, componentsById, now);
    }

    /**
     * Pre-creates all of a rule's actions and emits the satisfied-rule notification event. Shared by both the
     * reactive (metric/RX-open) path and the schedule-tick path below - the two paths differ only in how they
     * decide *whether* to fire, not in what happens once they do.
     */
    private void dispatchRuleActions(CoopState coop, RuleState rule, Map<String, ComponentState> componentsById, long now) {

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

                    // Ask the actuator what this frame targets rather than reaching into params by a
                    // device-specific key name - keeps the rule engine generic across device types.
                    PortCommand described = actuator.describeFrame(downlink);
                    if(described != null) {
                        PortActionHubEvent portEvent = new PortActionHubEvent(
                                actionComponent.getComponentId(), described.portIndex(), described.actionKey(), "RULE", "REQUESTED");
                        portEvent.setDt(now);
                        portEvent.setCoopId(coop.getCoopId());
                        provider.save(portEvent);
                    }
                }
            }
        }

        triggerStateRepository.upsert(rule.getRuleId(), ACTIVE);

        // Emit an event to notify the user that the rule was executed
        RuleSatisfiedHubEvent event = new RuleSatisfiedHubEvent();
        event.setDt(now);
        event.setCoopId(coop.getCoopId());
        event.setRuleId(rule.getRuleId());
        event.setContext(context(rule));
        provider.save(event);
    }

    /**
     * Ticked roughly once a second by PiRunner. Reactive (component-triggered) rules are handled entirely by
     * receive() above - this only drives schedule-triggered rules, which have no incoming event to react to.
     */
    @Override
    public void invoke() {

        CoopState coop = provider.getConfig();
        if(coop == null || coop.getRules() == null) {
            return;
        }

        Map<String, ComponentState> componentsById =
                Optional.ofNullable(coop.getComponents())
                        .orElseGet(List::of)
                        .stream()
                        .collect(Collectors.toMap(
                                ComponentState::getComponentId,
                                Function.identity(),
                                (a, b) -> a
                        ));

        LocalDateTime now = LocalDateTime.now();

        for(RuleState rule : coop.getRules()) {

            List<RuleScheduleTriggerState> schedules = rule.getScheduleTriggers();
            if(schedules == null || schedules.isEmpty()) {
                continue;
            }

            try {
                List<RuleScheduleTriggerState> due = schedules.stream()
                        .filter(trigger -> isScheduleDue(trigger, now))
                        .toList();

                if(!due.isEmpty()) {
                    dispatchRuleActions(coop, rule, componentsById, System.currentTimeMillis());
                    long firedAt = System.currentTimeMillis();
                    due.forEach(trigger -> scheduleTriggerStateRepository.upsert(trigger.getId(), firedAt));
                }
            } catch (Throwable t) {
                log.error("Failed to process schedule triggers for rule " + rule.getRuleId(), t);
            }
        }
    }

    /**
     * Determines whether a schedule trigger is due right now. Dedup is tracked per-trigger (not per-rule) via
     * ScheduleTriggerStateRepository so multiple schedule entries on one rule fire independently of each other,
     * and so the ~60 ticks a minute match stays true for don't cause repeat fires.
     */
    private boolean isScheduleDue(RuleScheduleTriggerState trigger, LocalDateTime now) {

        ScheduleFrequency frequency;
        try {
            frequency = ScheduleFrequency.valueOf(trigger.getFrequency());
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }

        ScheduleTriggerState state = scheduleTriggerStateRepository.findById(trigger.getId());
        Long lastFiredDt = state == null ? null : state.getLastFiredDt();
        int gap = Math.max(1, trigger.getGap());

        if(frequency == ScheduleFrequency.HOUR) {
            if(now.getMinute() != trigger.getMinute()) {
                return false;
            }
            if(lastFiredDt == null) {
                return true;
            }
            long hoursSince = ChronoUnit.HOURS.between(Instant.ofEpochMilli(lastFiredDt), toInstant(now));
            return hoursSince >= gap;
        }

        if(now.getHour() != trigger.getHour() || now.getMinute() != trigger.getMinute()) {
            return false;
        }

        if(frequency == ScheduleFrequency.DAY) {
            if(lastFiredDt == null) {
                return true;
            }
            LocalDate lastFiredDate = toLocalDate(lastFiredDt);
            long daysSince = ChronoUnit.DAYS.between(lastFiredDate, now.toLocalDate());
            return daysSince >= gap;
        }

        // Otherwise, the frequency names a specific day of the week - names match java.time.DayOfWeek exactly.
        DayOfWeek dayOfWeek;
        try {
            dayOfWeek = DayOfWeek.valueOf(frequency.name());
        } catch (IllegalArgumentException e) {
            return false;
        }

        if(now.getDayOfWeek() != dayOfWeek) {
            return false;
        }

        if(lastFiredDt == null) {
            return true;
        }

        // Once per calendar day - gap isn't used for weekday frequencies.
        return !toLocalDate(lastFiredDt).equals(now.toLocalDate());
    }

    private static Instant toInstant(LocalDateTime dt) {
        return dt.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static LocalDate toLocalDate(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    //TODO: If the operator is invalid, then should we return false or bubble up a failure?
    private boolean isRuleSatisfied(RuleState rule) {
        //TODO: Hard coding age limit to two hours should probably be configurable
        boolean componentTriggersSatisfied = rule.getComponentTriggers().stream().allMatch(trigger -> {
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

        boolean timeTriggersSatisfied = Optional.ofNullable(rule.getTimeTriggers())
                .orElseGet(List::of)
                .stream()
                .allMatch(this::isTimeConditionSatisfied);

        return componentTriggersSatisfied && timeTriggersSatisfied;
    }

    /**
     * Time conditions are ANDed alongside component triggers, evaluated the same reactive way (e.g. "moisture
     * below 40% AND before 8AM") - not a separate clock-driven firing path like ScheduledRuleTrigger.
     */
    private boolean isTimeConditionSatisfied(RuleTimeTriggerState trigger) {
        try {
            LocalTime now = LocalTime.now();
            double nowMinutes = now.getHour() * 60 + now.getMinute();
            double thresholdMinutes = trigger.getHour() * 60 + trigger.getMinute();
            return Operator.valueOf(trigger.getOperator()).evaluate(nowMinutes, thresholdMinutes);
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
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
