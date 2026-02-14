package coop.local.state;

import coop.local.PiContext;
import coop.shared.database.repository.*;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.pi.StateFactory;
import coop.shared.pi.events.HubEvent;
import coop.shared.pi.events.MetricReceived;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.projection.InboxMessageProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@EnableTransactionManagement
@Component
@ConditionalOnProperty(
        name = "coop.state.mode",
        havingValue = "database",
        matchIfMissing = true
)
public class DatabaseStateProvider extends LocalStateProvider {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private InboxMessageRepository inboxRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private PiContext piContext;

    @Autowired
    private StateFactory stateFactory;

    @Autowired
    @Qualifier("coop_id")
    private String coopId;

    @Override
    public void init() {
        refreshState();
    }

    @Override
    public void refreshState() {

        Pi pi = piRepository.findById(piContext.piId());
        Coop coop = coopRepository.findById(pi, coopId);
        this.put(stateFactory.forCoop(coop));
        if (this.getConfig() == null) {
            throw new IllegalStateException("Config for coop not found.");
        }
    }

    @Override
    public void save(HubEvent event) {
        if(event instanceof MetricReceived metric) {
            saveMetric(metric);
        }

        if(event instanceof RuleSatisfiedHubEvent ruleExecution) {
            saveRuleExecution(ruleExecution);
        }
    }

    private void saveRuleExecution(RuleSatisfiedHubEvent ruleExecution) {
        Pi pi = piRepository.findById(piContext.piId());
        InboxMessageProjection projection = new InboxMessageProjection(coopRepository, ruleRepository);
        projection.from(pi, ruleExecution).forEach(message -> {
            inboxRepository.persist(message);
        });
    }

    private void saveMetric(MetricReceived metric) {
        metricRepository.save(
                coop(),
                metric.getComponentId(),
                System.currentTimeMillis(),
                metric.getMetric(),
                metric.getValue());
    }

    private Coop coop() {
        if (this.getConfig() == null || this.getConfig().getCoopId() == null) {
            return null;
        }

        Pi pi = piRepository.findById(piContext.piId());
        return coopRepository.findById(pi, this.getConfig().getCoopId());
    }
}
