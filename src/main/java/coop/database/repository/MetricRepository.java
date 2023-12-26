package coop.database.repository;

import coop.database.table.CoopMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Repository
@EnableTransactionManagement
@Transactional
public class MetricRepository extends GenericRepository<CoopMetric> {
    @Override
    protected Class<CoopMetric> getObjClass() {
        return CoopMetric.class;
    }
}