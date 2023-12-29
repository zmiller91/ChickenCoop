package coop.database.repository;

import coop.database.table.Coop;
import coop.database.table.CoopMetric;
import coop.pi.metric.Metric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class MetricRepository extends AuthorizerScopedRepository<CoopMetric> {
    @Override
    protected Class<CoopMetric> getObjClass() {
        return CoopMetric.class;
    }

    public List<CoopMetric> findByMetric(Coop coop, String metric) {
        return this.query("FROM CoopMetric WHERE metric = :metric AND coop = :coop ORDER BY dt ASC", CoopMetric.class)
                .setParameter("metric", metric)
                .setParameter("coop", coop)
                .list();
    }
}