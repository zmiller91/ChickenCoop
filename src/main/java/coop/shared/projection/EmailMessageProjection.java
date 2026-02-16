package coop.shared.projection;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.rule.NotificationChannel;
import coop.shared.database.table.rule.Rule;
import coop.shared.database.table.rule.RuleNotification;
import coop.shared.database.table.rule.RuleNotificationRecipient;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.ses.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class EmailMessageProjection {

    private CoopRepository coopRepository;
    private RuleRepository ruleRepository;

    public List<SendEmailRequest> from(Pi pi, RuleSatisfiedHubEvent ruleExecution) {

        Coop coop = coopRepository.findById(pi, ruleExecution.getCoopId());
        if(coop == null) {
            return new ArrayList<>();
        }

        Rule rule = ruleRepository.findByCoopAndId(coop, ruleExecution.getRuleId());
        if(rule == null) {
            return new ArrayList<>();
        }


        List<RuleNotification> emailNotifications = rule.getNotifications()
                .stream()
                .filter(n -> NotificationChannel.EMAIL.equals(n.getChannel()))
                .toList();


        List<SendEmailRequest> emails = new ArrayList<>();
        for(RuleNotification notification : emailNotifications) {

            if(notification.getRecipients() == null) {
                continue;
            }

            for(RuleNotificationRecipient recipient : notification.getRecipients()) {

                if(recipient.getContact() == null || StringUtils.isEmpty(recipient.getContact().getEmail())) {
                    continue;
                }

                SendEmailRequest req = SendEmailRequest.builder()
                        .source("alerts@gnomelyhq.com")
                        .destination(Destination.builder().toAddresses(recipient.getContact().getEmail()).build())
                        .message(Message.builder()
                                .subject(Content.builder().data(String.format("Automation: %s", rule.getName())).charset("UTF-8").build())
                                .body(Body.builder()
                                        .html(Content.builder().data(getBody(rule, ruleExecution, true)).charset("UTF-8").build())
                                        .text(Content.builder().data(getBody(rule, ruleExecution, false)).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build();

                emails.add(req);

            }
        }

        return emails;
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
