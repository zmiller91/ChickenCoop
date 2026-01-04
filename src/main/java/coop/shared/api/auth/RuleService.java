package coop.shared.api.auth;

import com.google.gson.JsonParseException;
import coop.device.Action;
import coop.device.Actuator;
import coop.device.RuleSignal;
import coop.device.RuleSource;
import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.Coop;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if(rule == null || rule.getCoop() != coop) {
            throw new NotFound("Rule not found.");
        }

        return new GetRuleResponse(toDTO(rule));
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


        rule.setActions(actions);
        rule.setComponentTriggers(componentTriggers);

        ruleRepository.persist(rule);
        ruleRepository.flush();

        CoopState state = stateFactory.forCoop(coop);
        stateProvider.put(state);

        return new CreateRuleResponse(toDTO(rule));
    }


    private void verifyActions(RuleDTO rule) {

        List<RuleActionDTO> actions = rule.actions();
        if(actions == null || actions.isEmpty()) {
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
                rule.getActions().stream().map(RuleService::toDTO).toList()
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

    public record ListRulesResponse(List<RuleDTO> rules){};
    public record CreateRuleRequest(String coopId, RuleDTO rule){};
    public record CreateRuleResponse(RuleDTO rule){};
    public record ListRuleSourcesResponse(List<RuleComponentDTO> components, Map<String, SourceDTO> sources){}
    public record ListActuatorsResponse(List<RuleComponentDTO> components, Map<String, ActuatorDTO> actions){}
    public record GetRuleResponse(RuleDTO rule){};

    public record SignalDTO(String key){}
    public record SourceDTO(List<SignalDTO> signals){}

    public record RuleComponentDTO(String id, String name, String serialNumber, String type){};
    public record RuleActionDTO(String id, RuleComponentDTO component, String actionKey, Map<String, String> params){};
    public record ScheduleTriggerDTO(String id, String frequency, int hour, int minute, int gap){};
    public record ComponentTriggerDTO(String id, RuleComponentDTO component, String signal, double threshold, String operator){};
    public record RuleDTO(String id, String name, String status, List<ComponentTriggerDTO> componentTriggers, List<ScheduleTriggerDTO> scheduleTriggers, List<RuleActionDTO> actions){};
    public record ActuatorActionDTO(String key, String[] params){}
    public record ActuatorDTO(List<ActuatorActionDTO> actions){}

}
