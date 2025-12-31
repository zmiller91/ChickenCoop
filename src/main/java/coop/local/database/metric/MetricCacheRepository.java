package coop.local.database.metric;

import coop.local.database.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class MetricCacheRepository extends BaseRepository {

    public MetricCacheEntry findRecent(String componentId, String metric, Duration ageLimit) {
        long cutoff = System.currentTimeMillis() - ageLimit.toMillis();

        return sessionFactory.getCurrentSession().createQuery("""
            SELECT m
            FROM MetricCacheEntry m
            WHERE m.componentId = :componentId
              AND m.metric = :metric
              AND m.updatedAtMs > :cutoff
            """, MetricCacheEntry.class)
                .setParameter("componentId", componentId)
                .setParameter("metric", metric)
                .setParameter("cutoff", cutoff)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public void upsert(MetricCacheEntry entry) {
        sessionFactory.getCurrentSession().createNativeQuery("""
                INSERT INTO metric_cache (component_id, metric, metric_value, updated_at_ms)
                VALUES (:componentId, :metric, :value, :updatedAtMs)
                ON DUPLICATE KEY UPDATE
                    metric_value = VALUES(metric_value),
                    updated_at_ms = VALUES(updated_at_ms)
                """)
                .setParameter("componentId", entry.getComponentId())
                .setParameter("metric", entry.getMetric())
                .setParameter("value", entry.getValue())
                .setParameter("updatedAtMs", System.currentTimeMillis())
                .executeUpdate();
    }

}