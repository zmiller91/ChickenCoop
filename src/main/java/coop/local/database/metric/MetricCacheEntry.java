package coop.local.database.metric;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "metric_cache")
public class MetricCacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "metric")
    private String metric;

    @Column(name = "component_id")
    private String componentId;

    @Column(name = "metric_value")
    private Double value;

    @Column(name = "updated_at_ms", nullable = false)
    private Long updatedAtMs;

    @PrePersist
    @PreUpdate
    void touchTimestamp() {
        updatedAtMs = System.currentTimeMillis();
    }
}