package coop.local.state;

import coop.local.PiContext;
import coop.shared.database.repository.*;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.pi.StateFactory;
import coop.shared.pi.metric.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@EnableTransactionManagement
@Component
public class DatabaseStateProvider extends LocalStateProvider {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ComponentSerialRepository componentSerialRepository;

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
    public void save(Metric metric) {
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
