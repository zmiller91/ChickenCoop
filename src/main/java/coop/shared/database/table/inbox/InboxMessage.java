package coop.shared.database.table.inbox;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(
        name = "inbox_message",
        indexes = {
                @Index(name = "idx_msg_coop_created", columnList = "COOP_ID,CREATED_TS"),
                @Index(name = "idx_message_unread_count", columnList = "COOP_ID,READ_TS,ARCHIVED_TS,DELETED_TS")
        }
)
public class InboxMessage implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(name = "INBOX_MESSAGE_ID", length = 32, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COOP_ID", nullable = false)
    private Coop coop;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERITY", length = 16, nullable = false)
    private InboxSeverity severity;

    @Column(name = "CREATED_TS", nullable = false, updatable = false)
    private Instant createdTs;

    @Column(name = "READ_TS")
    private Instant readTs;

    @Column(name = "ARCHIVED_TS")
    private Instant archivedTs;

    @Column(name = "DELETED_TS")
    private Instant deletedTs;

    @Column(name = "SUBJECT", length = 255, nullable = false)
    private String subject;

    @Lob
    @Column(name = "BODY_TEXT")
    private String bodyText;

    @Lob
    @Column(name = "BODY_HTML", columnDefinition = "MEDIUMTEXT")
    private String bodyHtml;

    @PrePersist
    protected void onCreate() {
        if (createdTs == null) {
            createdTs = Instant.now();
        }
    }

    // Convenience helpers (optional)
    @Transient
    public boolean isUnread() {
        return readTs == null && archivedTs == null && deletedTs == null;
    }

    public void markRead() {
        this.readTs = Instant.now();
    }

    public void archive() {
        this.archivedTs = Instant.now();
    }

    public void delete() {
        this.deletedTs = Instant.now();
    }

    @Override
    public User getUser() {
        return coop.getUser();
    }

    @Override
    public Pi getPi() {
        return coop.getPi();
    }
}
