package coop.local.state;

import coop.local.PiContext;
import coop.shared.database.repository.*;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.database.table.component.PortActionLogEntry;
import coop.shared.database.table.component.PortActionSource;
import coop.shared.database.table.component.PortActionStatus;
import coop.shared.database.table.rule.Rule;
import coop.shared.database.table.rule.RuleExecutionLogEntry;
import coop.shared.pi.StateFactory;
import coop.shared.pi.events.HubEvent;
import coop.shared.pi.events.MetricReceived;
import coop.shared.pi.events.RuleSatisfiedHubEvent;
import coop.shared.pi.events.PortActionHubEvent;
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
    private ComponentRepository componentRepository;

    @Autowired
    private PortActionLogRepository portActionLogRepository;

    @Autowired
    private RuleExecutionLogRepository ruleExecutionLogRepository;

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

        if(event instanceof PortActionHubEvent portAction) {
            savePortAction(portAction);
        }
    }

    private void savePortAction(PortActionHubEvent event) {
        Pi pi = piRepository.findById(piContext.piId());
        coop.shared.database.table.component.Component component = componentRepository.findById(pi, event.getComponentId());
        if(component == null) {
            return;
        }

        PortActionLogEntry entry = new PortActionLogEntry();
        entry.setComponent(component);
        entry.setPortIndex(event.getPortIndex());
        entry.setActionKey(event.getActionKey());
        entry.setSource(event.getSource() != null ? PortActionSource.valueOf(event.getSource()) : null);
        entry.setStatus(PortActionStatus.valueOf(event.getStatus()));
        entry.setCreatedAt(event.getDt());
        portActionLogRepository.persist(entry);
    }

    private void saveRuleExecution(RuleSatisfiedHubEvent ruleExecution) {
        Pi pi = piRepository.findById(piContext.piId());

        Coop coop = coopRepository.findById(pi, coopId);
        if(coop == null) {
            return;
        }

        Rule rule = ruleRepository.findByCoopAndId(coop, ruleExecution.getRuleId());
        if(rule == null) {
            return;
        }

        RuleExecutionLogEntry entry = new RuleExecutionLogEntry();
        entry.setRule(rule);
        entry.setCreatedAt(ruleExecution.getDt());
        ruleExecutionLogRepository.persist(entry);

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
