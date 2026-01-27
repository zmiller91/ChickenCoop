package coop.shared.projection;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.inbox.InboxMessage;
import coop.shared.database.table.rule.NotificationChannel;
import coop.shared.database.table.rule.Rule;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

@AllArgsConstructor
public class InboxMessageProjection {

    private CoopRepository coopRepository;
    private RuleRepository ruleRepository;

    public List<InboxMessage> from(Pi pi, RuleSatisfiedHubEvent ruleExecution) {

        Coop coop = coopRepository.findById(pi, ruleExecution.getCoopId());
        if(coop == null) {
            return new ArrayList<>();
        }

        Rule rule = ruleRepository.findByCoopAndId(coop, ruleExecution.getRuleId());
        if(rule == null) {
            return new ArrayList<>();
        }

        return rule.getNotifications()
                .stream()
                .filter(n -> NotificationChannel.INBOX.equals(n.getChannel()))
                .map(n -> {
                    InboxMessage message = new InboxMessage();
                    message.setCoop(coop);
                    message.setSubject(String.format("Automation: %s", rule.getName()));
                    message.setSeverity(n.getLevel());
                    message.setBodyText(getBody(rule, ruleExecution, false));
                    message.setBodyHtml(getBody(rule, ruleExecution, true));
                    return message;
                }).toList();

    }


    private String getBody(Rule rule, RuleSatisfiedHubEvent event, boolean forHtml) {

        // Create a quick lookup so it can be referenced from below
        Map<String, Component> componentLookup = new HashMap<>();
        rule.getComponentTriggers().forEach(trigger -> {
            if(trigger.getComponent() != null) {
                Component component = trigger.getComponent();
                if(component.getComponentId() != null) {
                    componentLookup.put(component.getComponentId(), trigger.getComponent());
                }
            }
        });

        List<String> bodyLines = new ArrayList<>(List.of(
                "All criteria for the " + rule.getName() + " automation were satisfied.",
                "",
                "Raw criteria:",
                ""));

        List<String> context = event.getContext().entrySet()
                .stream()
                .map(e -> {
                    Component component = componentLookup.get(e.getKey());
                    String name = ObjectUtils.firstNonNull(component.getName(), e.getKey());
                    return name + ": " + e.getValue();
                }).toList();

        bodyLines.addAll(context);
        return String.join(forHtml ? "<br/>" : "\n", bodyLines);
    }
}
