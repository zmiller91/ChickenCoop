package coop.local.database;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="frame_id")
    private String frameId;

    @Column(name="dedupe_key")
    private String dedupeKey;

    @Column(name="component_id")
    private String componentId;

    @Column(name="status_en")
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name="command")
    private String command;

    @Column(name="created_at")
    private long createdAt;

    @Column(name="status_rank")
    private int rank;

    @Column(name="expire_at")
    private long expireAt;

    public boolean isExpired() {
        return System.currentTimeMillis() > getExpireAt();
    }
}
