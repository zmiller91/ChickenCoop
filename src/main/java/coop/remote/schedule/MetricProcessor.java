package coop.remote.schedule;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.MetricRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.pi.events.MetricReceived;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricProcessor {

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Transactional
    public void process(Pi pi, MetricReceived metric) {
        Coop coop = coopRepository.findById(pi, metric.getCoopId());
        if(coop != null) {
            metricRepository.save(
                    coop,
                    metric.getComponentId(),
                    metric.getDt(),
                    metric.getMetric(),
                    metric.getValue());
        }
    }

}
