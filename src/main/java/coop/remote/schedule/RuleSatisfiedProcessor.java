package coop.remote.schedule;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.InboxMessageRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.Pi;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.projection.EmailMessageProjection;
import coop.shared.projection.InboxMessageProjection;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ses.SesClient;

@Service
@Log4j2
public class RuleSatisfiedProcessor {

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private InboxMessageRepository inboxRepository;

    @Autowired
    private SesClient ses;

    @Transactional
    public void process(Pi pi, RuleSatisfiedHubEvent ruleExecution) {

        InboxMessageProjection inboxProjection = new InboxMessageProjection(coopRepository, ruleRepository);
        inboxProjection.from(pi, ruleExecution).forEach(message -> {
            inboxRepository.persist(message);
        });

        EmailMessageProjection emailProjection = new EmailMessageProjection(coopRepository, ruleRepository);
        emailProjection.from(pi, ruleExecution).forEach(email -> {
            try {
                ses.sendEmail(email);
            } catch (Throwable t) {
                log.error("Failed to send email.", t);
            }
        });
    }






}
