package coop.remote.schedule;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.InboxMessageRepository;
import coop.shared.database.repository.RuleRepository;
import coop.shared.database.table.Pi;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.projection.InboxMessageProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleSatisfiedProcessor {

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private InboxMessageRepository inboxRepository;

    @Transactional
    public void process(Pi pi, RuleSatisfiedHubEvent ruleExecution) {
        InboxMessageProjection projection = new InboxMessageProjection(coopRepository, ruleRepository);
        projection.from(pi, ruleExecution).forEach(message -> {
            inboxRepository.persist(message);
        });
    }
}
