package coop.local.database.job;

import coop.local.database.downlink.Downlink;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="dedupe_key")
    private String dedupeKey;

    @Column(name="component_id")
    private String componentId;

    @Column(name="status_en")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name="created_at")
    private long createdAt;

    @Column(name="status_rank")
    private int rank;

    @Column(name="expire_at")
    private long expireAt;

    @Column(name="status_update_ts")
    private long statusUpdateTs;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name="downlink_id", nullable = false, unique = true)
    private Downlink downlink;

    @PrePersist
    void prePersist() {
        if (statusUpdateTs == 0) statusUpdateTs = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > getExpireAt();
    }
}
