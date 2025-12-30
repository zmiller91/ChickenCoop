package coop.shared.api.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import coop.device.Actuator;
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
            trigger.setMetric(dto.metric);
            trigger.setRule(rule);
            return trigger;

        }).toList();

        List<RuleAction> actions = request.rule.actions.stream().map(dto -> {

            Component component = componentRepository.findById(userContext.getCurrentUser(), dto.component.id);
            RuleAction action = new RuleAction();
            action.setComponent(component);
            action.setAction(dto.action);
            action.setRule(rule);
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

            if(action.action == null || action.action.isEmpty()) {
                throw new BadRequest("Empty action body.");
            }

            try {
                JsonObject body = JsonParser.parseString(action.action()).getAsJsonObject();
                if(!((Actuator) component.getSerial().getDeviceType().getDevice()).validateCommand(body)) {
                    throw new BadRequest("Invalid action body.");
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
                action.getAction()
        );
    }

    private static RuleComponentDTO toDTO(Component component) {
        return new RuleComponentDTO(component.getComponentId(),
                component.getName(),
                component.getSerial().getSerialNumber(),
                component.getSerial().getDeviceType().name());
    }

    public record ListRulesResponse(List<RuleDTO> rules){};
    public record CreateRuleRequest(String coopId, RuleDTO rule){};
    public record CreateRuleResponse(RuleDTO rule){};

    public record RuleComponentDTO(String id, String name, String serialNumber, String type){};
    public record RuleActionDTO(String id, RuleComponentDTO component, String action){};
    public record ScheduleTriggerDTO(String id, String frequency, int hour, int minute, int gap){};
    public record ComponentTriggerDTO(String id, RuleComponentDTO component, String metric, double threshold, String operator){};
    public record RuleDTO(String id, String name, String status, List<ComponentTriggerDTO> componentTriggers, List<ScheduleTriggerDTO> scheduleTriggers, List<RuleActionDTO> actions){};

}
