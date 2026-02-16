package coop.shared.api.auth;

import com.google.gson.JsonParseException;
import coop.device.Action;
import coop.device.Actuator;
import coop.device.RuleSignal;
import coop.device.RuleSource;
import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.ContactRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.Contact;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Severity;
import coop.shared.database.table.Status;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.rule.*;
import coop.shared.exception.BadRequest;
import coop.shared.exception.NotFound;
import coop.shared.pi.StateFactory;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.security.AuthContext;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.stream.Streams;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static coop.shared.database.table.rule.NotificationChannel.EMAIL;
import static coop.shared.database.table.rule.NotificationChannel.TEXT;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/rule")
public class RuleService {

    @Autowired
    private AuthContext userContext;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private StateProvider stateProvider;

    @Autowired
    private StateFactory stateFactory;

    @GetMapping("{coopId}/rulesources/list")
    public ListRuleSourcesResponse listRuleSources(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<Component> sources = componentRepository.findByCoop(coop)
                .stream()
                .filter(c -> c.getSerial().getDeviceType().getDevice() instanceof RuleSource)
                .toList();


        Map<String, SourceDTO> sourceTypes = sources.stream()
                .map(s -> s.getSerial().getDeviceType())
                .distinct()
                .collect(Collectors.toMap(Enum::name, dt -> toDTO((RuleSource) dt.getDevice())));

        return new ListRuleSourcesResponse(
                sources.stream().map(RuleService::toDTO).toList(),
                sourceTypes
        );
    }

    @GetMapping("{coopId}/actuators/list")
    public ListActuatorsResponse ListActuators(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<Component> actuators = componentRepository.findByCoop(coop)
                .stream()
                .filter(c -> c.getSerial().getDeviceType().getDevice() instanceof Actuator)
                .toList();

        Map<String, ActuatorDTO> actionMap = actuators.stream()
                .map(a -> a.getSerial().getDeviceType())
                .distinct()
                .collect(Collectors.toMap(Enum::name, dt -> toDTO((Actuator) dt.getDevice())));

        return new ListActuatorsResponse(
                actuators.stream().map(RuleService::toDTO).toList(),
                actionMap
        );
    }

    @GetMapping("{coopId}/{ruleId}")
    public GetRuleResponse getRule(
            @PathVariable("coopId") String coopId,
            @PathVariable("ruleId") String ruleId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        Rule rule = ruleRepository.findById(coop.getUser(), ruleId);
        if(rule == null || !rule.getCoop().equals(coop)) {
            throw new NotFound("Rule not found.");
        }

        return new GetRuleResponse(toDTO(rule));
    }

    @DeleteMapping("{coopId}/{ruleId}")
    public void delete(
            @PathVariable("coopId") String coopId,
            @PathVariable("ruleId") String ruleId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        Rule rule = ruleRepository.findById(coop.getUser(), ruleId);
        if(rule == null || !rule.getCoop().equals(coop)) {
            throw new NotFound("Rule not found.");
        }

        ruleRepository.delete(rule);
        ruleRepository.flush();

        CoopState state = stateFactory.forCoop(coop);
        stateProvider.put(state);
    }

    @PutMapping("{coopId}/{ruleId}")
    public void updateRule(
            @PathVariable("coopId") String coopId,
            @PathVariable("ruleId") String ruleId,
            @RequestBody UpdateRuleRequest request) {

        if(request.rule == null) {
            throw new BadRequest("Rule is empty.");
        }

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        Rule rule = ruleRepository.findById(coop.getUser(), ruleId);
        if(rule == null || !rule.getCoop().equals(coop)) {
            throw new NotFound("Rule not found.");
        }

        if(!ruleId.equals(request.rule.id)) {
            throw new BadRequest("Invalid resource.");
        }

        rule.setName(request.rule.name);

        // Actions
        deleteActionsInUpdate(rule, request);
        addActionsInUpdate(rule, request);
        updateActionsInUpdate(rule, request);

        // Component triggers
        deleteComponentTriggersInUpdate(rule, request);
        addComponentTriggersInUpdate(rule, request);
        updateComponentTriggersInUpdate(rule, request);

        // Notifications
        deleteNotificationsInUpdate(rule, request);
        addNotificationsInUpdate(rule, request);
        updateNotificationsInUpdate(rule, request);

        ruleRepository.persist(rule);
        ruleRepository.flush();

        CoopState state = stateFactory.forCoop(coop);
        stateProvider.put(state);
    }

    private void updateComponentTriggersInUpdate(Rule rule, UpdateRuleRequest request) {
        Map<String, ComponentRuleTrigger> triggersById = rule.getComponentTriggers().stream()
                .filter(a -> !Strings.isBlank(a.getId()))
                .collect(Collectors.toMap(ComponentRuleTrigger::getId, Function.identity()));

        for (ComponentTriggerDTO dto : request.rule().componentTriggers()) {
            if (Strings.isBlank(dto.id)) continue;

            ComponentRuleTrigger trigger = triggersById.get(dto.id);
            if (trigger == null) {
                throw new NotFound("Component trigger not found: " + dto.id);
            }

            Component component = componentRepository.findById(rule.getPi(), dto.component.id);
            if (component == null || !component.getCoop().equals(rule.getCoop())) {
                throw new NotFound("Component not found: " + dto.component.id);
            }

            trigger.setComponent(component);
            trigger.setOperator(Operator.valueOf(dto.operator));
            trigger.setThreshold(dto.threshold);
            trigger.setMetric(dto.signal);
        }
    }

    private void addComponentTriggersInUpdate(Rule rule, UpdateRuleRequest request) {

        List<ComponentRuleTrigger> newComponentTriggers = request.rule().componentTriggers()
                .stream()
                .filter( a -> a.id == null)
                .map(dto -> {

                    Component component = componentRepository.findById(rule.getPi(), dto.component.id);
                    if (component == null || !component.getCoop().equals(rule.getCoop())) {
                        throw new NotFound("Component not found: " + dto.component.id);
                    }

                    ComponentRuleTrigger trigger = new ComponentRuleTrigger();
                    trigger.setComponent(component);
                    trigger.setOperator(Operator.valueOf(dto.operator));
                    trigger.setThreshold(dto.threshold);
                    trigger.setMetric(dto.signal);
                    trigger.setRule(rule);
                    return trigger;

                }).toList();

        rule.getComponentTriggers().addAll(newComponentTriggers);
    }

    private void deleteComponentTriggersInUpdate(Rule rule, UpdateRuleRequest request) {
        Set<String> idsInRequest = request.rule().componentTriggers().stream()
                .map(ComponentTriggerDTO::id)
                .filter(id -> !Strings.isBlank(id))
                .collect(Collectors.toSet());

        List<ComponentRuleTrigger> toRemove = rule.getComponentTriggers().stream()
                .filter(t -> !idsInRequest.contains(t.getId()))
                .toList();

        toRemove.forEach(rule::removeComponentTrigger);
    }


    private void updateActionsInUpdate(Rule rule, UpdateRuleRequest request) {
        Map<String, RuleAction> actionsById = rule.getActions().stream()
                .filter(a -> !Strings.isBlank(a.getId()))
                .collect(Collectors.toMap(RuleAction::getId, Function.identity()));

        for (RuleActionDTO dto : request.rule().actions()) {
            if (Strings.isBlank(dto.id)) continue;

            RuleAction action = actionsById.get(dto.id);
            if (action == null) {
                throw new NotFound("Rule action not found: " + dto.id);
            }

            Component component = componentRepository.findById(rule.getPi(), dto.component.id);
            if (component == null || !component.getCoop().equals(rule.getCoop())) {
                throw new NotFound("Component not found: " + dto.component.id);
            }

            // Need to make sure any existing params are updated, instead of inserted as an insert can cause a
            // unique key constraint violation
            Map<String, RuleActionParam> paramsByKey = action.getParams()
                    .stream()
                    .collect(Collectors.toMap(RuleActionParam::getKey, Function.identity()));

            for(Map.Entry<String, String> param : dto.params.entrySet()) {

                if(paramsByKey.containsKey(param.getKey())) {
                    paramsByKey.get(param.getKey()).setValue(param.getValue());

                } else {
                    RuleActionParam newParam = new RuleActionParam();
                    newParam.setKey(param.getKey());
                    newParam.setValue(param.getValue());
                    newParam.setRuleAction(action);
                    action.getParams().add(newParam);
                }

            }

            action.setActionKey(dto.actionKey);
            action.setComponent(component);
        }
    }

    private void addActionsInUpdate(Rule rule, UpdateRuleRequest request) {

        List<RuleAction> newActions = request.rule().actions()
                .stream()
                .filter( a -> a.id == null)
                .map(dto -> {

                    Component component = componentRepository.findById(rule.getPi(), dto.component.id);
                    if(component == null || !component.getCoop().equals(rule.getCoop())) {
                        throw new NotFound("Component not found: " + dto.component.id);
                    }

                    RuleAction action = new RuleAction();
                    action.setActionKey(dto.actionKey);
                    action.setParamsMap(dto.params);
                    action.setComponent(component);
                    action.setRule(rule);
                    return action;

                }).toList();

        rule.getActions().addAll(newActions);
    }

    private void deleteActionsInUpdate(Rule rule, UpdateRuleRequest request) {
        Set<String> idsInRequest = request.rule().actions().stream()
                .map(a -> a.id)
                .filter(id -> !Strings.isBlank(id))
                .collect(Collectors.toSet());

        List<RuleAction> toRemove = rule.getActions().stream()
                .filter(a -> !idsInRequest.contains(a.getId()))
                .toList();

        toRemove.forEach(rule::removeAction);
    }

    private void updateNotificationsInUpdate(Rule rule, UpdateRuleRequest request) {
        Map<String, RuleNotification> notificationsById = rule.getNotifications().stream()
                .filter(a -> !Strings.isBlank(a.getId()))
                .collect(Collectors.toMap(RuleNotification::getId, Function.identity()));

        for (RuleNotificationDTO dto : request.rule().notifications()) {
            if (Strings.isBlank(dto.id)) continue;

            RuleNotification notification = notificationsById.get(dto.id);
            if (notification == null) {
                throw new NotFound("Rule notification not found: " + dto.id);
            }

            notification.setType(NotificationType.valueOf(dto.type));
            notification.setLevel(Severity.valueOf(dto.level));
            notification.setMessage(dto.message);

        }
    }

    private void addNotificationsInUpdate(Rule rule, UpdateRuleRequest request) {

        List<RuleNotification> newNotifications = request.rule().notifications()
                .stream()
                .filter( n -> n.id == null)
                .map(dto -> {

                    RuleNotification notification = new RuleNotification();
                    notification.setType(NotificationType.valueOf(dto.type));
                    notification.setLevel(Severity.valueOf(dto.level));
                    notification.setChannel(NotificationChannel.valueOf(dto.channel));
                    notification.setRule(rule);
                    notification.setMessage(dto.message);

                    if(EMAIL.equals(notification.getChannel()) || TEXT.equals(notification.getChannel())) {
                        dto.recipients().forEach(recipient -> {
                            Contact contact = contactRepository.findByIdAndCoop(rule.getCoop(), recipient.id());
                            notification.addRecipient(contact);
                        });
                    }

                    return notification;

                }).toList();

        newNotifications.forEach(rule::addNotification);
    }

    private void deleteNotificationsInUpdate(Rule rule, UpdateRuleRequest request) {
        Set<String> idsInRequest = request.rule().notifications().stream()
                .map(n -> n.id)
                .filter(id -> !Strings.isBlank(id))
                .collect(Collectors.toSet());

        List<RuleNotification> toRemove = rule.getNotifications().stream()
                .filter(a -> !idsInRequest.contains(a.getId()))
                .toList();

        toRemove.forEach(rule::removeNotification);
    }


    @PostMapping("/create")
    public CreateRuleResponse create(@RequestBody CreateRuleRequest request) {
        
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), request.coopId());
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }
        
        if(request.rule() == null) {
            throw new BadRequest("Rule required.");
        }

        verifyComponentTriggers(request.rule);
        verifyActions(request.rule);
        verifyNotifications(coop, request.rule);

        Rule rule = new Rule();
        rule.setCoop(coop);
        rule.setStatus(Status.ACTIVE);
        rule.setName(request.rule.name);

        List<ComponentRuleTrigger> componentTriggers = request.rule.componentTriggers.stream().map(dto -> {

            Component component = componentRepository.findById(userContext.getCurrentUser(), dto.component.id);

            ComponentRuleTrigger trigger = new ComponentRuleTrigger();
            trigger.setComponent(component);
            trigger.setOperator(Operator.valueOf(dto.operator));
            trigger.setThreshold(dto.threshold);
            trigger.setMetric(dto.signal);
            trigger.setRule(rule);
            return trigger;

        }).toList();

        List<RuleAction> actions = request.rule.actions.stream().map(dto -> {

            Component component = componentRepository.findById(userContext.getCurrentUser(), dto.component.id);

            RuleAction action = new RuleAction();
            action.setComponent(component);
            action.setActionKey(dto.actionKey);
            action.setRule(rule);
            action.setParams(dto.params
                    .entrySet()
                    .stream()
                    .map(p -> new RuleActionParam(action, p.getKey(), p.getValue()))
                    .collect(Collectors.toSet()));

            return action;

        }).toList();

        List<RuleNotification> notifications = request.rule.notifications.stream().map(dto -> {

            RuleNotification notification = new RuleNotification();
            notification.setType(NotificationType.valueOf(dto.type));
            notification.setLevel(Severity.valueOf(dto.level));
            notification.setChannel(NotificationChannel.valueOf(dto.channel));
            notification.setRule(rule);
            notification.setMessage(dto.message);

            for(ContactService.ContactDTO contactDTO : dto.recipients()) {

                Contact contact = contactRepository.findByIdAndCoop(coop, contactDTO.id());

                RuleNotificationRecipientId id = new RuleNotificationRecipientId();
                id.setContactId(contact.getId());

                RuleNotificationRecipient recipient = new RuleNotificationRecipient();
                recipient.setContact(contact);
                recipient.setId(id);

                notification.addRecipient(recipient);
            }

            return notification;
        }).toList();


        rule.setActions(actions);
        rule.setComponentTriggers(componentTriggers);
        rule.setNotifications(notifications);

        ruleRepository.persist(rule);
        ruleRepository.flush();

        CoopState state = stateFactory.forCoop(coop);
        stateProvider.put(state);

        return new CreateRuleResponse(toDTO(rule));
    }

    private void verifyNotifications(Coop coop, RuleDTO rule) {
        List<RuleNotificationDTO> notifications = rule.notifications();
        for(RuleNotificationDTO notification : notifications) {

            if(StringUtils.isEmpty(notification.type())) {
                throw new BadRequest("Notification type is missing.");
            }

            if(StringUtils.isEmpty(notification.level())) {
                throw new BadRequest("Notification level is missing.");
            }

            if(StringUtils.isEmpty(notification.channel())) {
                throw new BadRequest("Channel level is missing.");
            }

            if (Streams.of(NotificationChannel.values())
                    .map(Enum::name)
                    .noneMatch(channel -> channel.equals(notification.channel()))) {

                throw new BadRequest("Unknown notification channel.");
            }

            if (Streams.of(Severity.values())
                    .map(Enum::name)
                    .noneMatch(level -> level.equals(notification.level()))) {

                throw new BadRequest("Unknown notification level.");
            }

            if (Streams.of(NotificationType.values())
                    .map(Enum::name)
                    .noneMatch(type -> type.equals(notification.type()))) {

                throw new BadRequest("Unknown notification type.");
            }

            if(notification.recipients() != null && !notification.recipients().isEmpty()) {

                for(ContactService.ContactDTO contactDTO : notification.recipients()) {

                    if(contactDTO == null || contactDTO.id() == null) {
                        throw new BadRequest("Recipients must have contact information.");
                    }

                    Contact contact = contactRepository.findByIdAndCoop(coop, contactDTO.id());
                    if(contact == null) {
                        throw new BadRequest("Contact not found.");
                    }

                }
            }
        }
    }

    private void verifyActions(RuleDTO rule) {

        List<RuleActionDTO> actions = rule.actions();
        if(actions == null) {
            throw new BadRequest("Action must exist.");
        }

        for(RuleActionDTO action : actions) {
            RuleComponentDTO dto = action.component;
            if(dto == null || dto.id == null) {
                throw new BadRequest("Actions must have a component associated with them.");
            }

            Component component = componentRepository.findById(userContext.getCurrentUser(), dto.id);
            if(component == null) {
                throw new BadRequest("Component not found for user.");
            }

            if(!(component.getSerial().getDeviceType().getDevice() instanceof Actuator)) {
                throw new BadRequest("Only actuator components can be used in actions.");
            }

            if(action.params == null || action.params.isEmpty()) {
                throw new BadRequest("Empty params body.");
            }

            try {

                if(!((Actuator) component.getSerial().getDeviceType().getDevice()).validateCommand(action.actionKey(), action.params())) {
                    throw new BadRequest("Invalid params body.");
                }
            } catch (JsonParseException | IllegalStateException e) {
                throw new BadRequest("Action body is not valid json.");
            }

        }
    }

    private void verifyComponentTriggers(RuleDTO rule) {

        List<ComponentTriggerDTO> componentTriggers = ObjectUtils.firstNonNull(rule.componentTriggers(), Collections.emptyList());
        for(ComponentTriggerDTO trigger :  componentTriggers) {
            RuleComponentDTO dto = trigger.component;
            if(dto == null || dto.id == null) {
                throw new BadRequest("Component triggers must have a component associated with them.");
            }

            Component component = componentRepository.findById(userContext.getCurrentUser(), dto.id);
            if(component == null) {
                throw new BadRequest("Component not found for user.");
            }
        }
    }
    
    @GetMapping("/{coopId}/list")
    public ListRulesResponse list(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<Rule> rules = ruleRepository.findByCoop(coop);
        List<RuleDTO> ruleDTOS =  rules.stream().map(RuleService::toDTO).toList();
        return new ListRulesResponse(ruleDTOS);
    }

    private static RuleDTO toDTO(Rule rule) {
        return new RuleDTO(
                rule.getId(),
                rule.getName(),
                rule.getStatus().name(),
                rule.getComponentTriggers().stream().map(RuleService::toDTO).toList(),
                rule.getScheduleTriggers().stream().map(RuleService::toDTO).toList(),
                rule.getActions().stream().map(RuleService::toDTO).toList(),
                rule.getNotifications().stream().map(RuleService::toDTO).toList()
        );
    }

    private static ScheduleTriggerDTO toDTO(ScheduledRuleTrigger trigger) {
        return  new ScheduleTriggerDTO(
                trigger.getId(),
                trigger.getFrequency().name(),
                trigger.getHour(),
                trigger.getMinute(),
                trigger.getGap()
        );
    }

    private static ComponentTriggerDTO toDTO(ComponentRuleTrigger trigger) {
        return new ComponentTriggerDTO(
                trigger.getId(),
                toDTO(trigger.getComponent()),
                trigger.getMetric(),
                trigger.getThreshold(),
                trigger.getOperator().name()
        );
    }

    private static RuleActionDTO toDTO(RuleAction action) {
        return new RuleActionDTO(
                action.getId(),
                toDTO(action.getComponent()),
                action.getActionKey(),
                action.getParamsMap()
        );
    }

    private static RuleComponentDTO toDTO(Component component) {
        return new RuleComponentDTO(component.getComponentId(),
                component.getName(),
                component.getSerial().getSerialNumber(),
                component.getSerial().getDeviceType().name());
    }

    private static SourceDTO toDTO(RuleSource source) {
        return new SourceDTO(source.getRuleMetrics().stream().map(RuleService::toDTO).toList());
    }

    private static SignalDTO toDTO(RuleSignal rs) {
        return new SignalDTO(rs.getKey());
    }

    private static ActuatorDTO toDTO(Actuator actuator) {
        return new ActuatorDTO(actuator.getActions().stream().map(RuleService::toDTO).toList());
    }

    private static ActuatorActionDTO toDTO(Action action) {
        return new ActuatorActionDTO(
                action.getKey(),
                action.getParams()
        );
    }

    private static RuleNotificationRecipientDTO toDTO(RuleNotificationRecipient recipient) {
        return new RuleNotificationRecipientDTO(
                ContactService.toDTO(recipient.getContact())
        );
    }

    private static RuleNotificationDTO toDTO(RuleNotification notification) {
        return new RuleNotificationDTO(
                notification.getId(),
                notification.getType().name(),
                notification.getLevel().name(),
                notification.getChannel().name(),
                notification.getMessage(),
                notification.getRecipients().stream().map(RuleNotificationRecipient::getContact).map(ContactService::toDTO).toList()
        );
    }

    public record ListRulesResponse(List<RuleDTO> rules){};
    public record CreateRuleRequest(String coopId, RuleDTO rule){};
    public record CreateRuleResponse(RuleDTO rule){};
    public record ListRuleSourcesResponse(List<RuleComponentDTO> components, Map<String, SourceDTO> sources){}
    public record ListActuatorsResponse(List<RuleComponentDTO> components, Map<String, ActuatorDTO> actions){}
    public record GetRuleResponse(RuleDTO rule){};
    public record UpdateRuleRequest(RuleDTO rule) { }

    public record SignalDTO(String key){}
    public record SourceDTO(List<SignalDTO> signals){}

    public record RuleComponentDTO(String id, String name, String serialNumber, String type){};
    public record RuleActionDTO(String id, RuleComponentDTO component, String actionKey, Map<String, String> params){};
    public record ScheduleTriggerDTO(String id, String frequency, int hour, int minute, int gap){};
    public record ComponentTriggerDTO(String id, RuleComponentDTO component, String signal, double threshold, String operator){};
    public record RuleDTO(String id, String name, String status, List<ComponentTriggerDTO> componentTriggers, List<ScheduleTriggerDTO> scheduleTriggers, List<RuleActionDTO> actions, List<RuleNotificationDTO> notifications){};
    public record ActuatorActionDTO(String key, String[] params){}
    public record ActuatorDTO(List<ActuatorActionDTO> actions){}

    public record RuleNotificationDTO(String id, String type, String level, String channel, String message, List<ContactService.ContactDTO> recipients){}
    public record RuleNotificationRecipientDTO(ContactService.ContactDTO contact){};

}
