package coop.shared.database.table.rule;

import coop.shared.database.table.Contact;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rule_notification")
public class RuleNotification {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(name = "RULE_NOTIFICATION_ID", length = 32, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RULE_ID", nullable = false)
    private Rule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_TYPE", length = 32, nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_LEVEL", length = 16, nullable = false)
    private NotificationLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "NOTIFICATION_CHANNEL", length = 16, nullable = false)
    private NotificationChannel channel;

    @Column(name = "MESSAGE", length = 512)
    private String message;

    @OneToMany(
            mappedBy = "notification",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RuleNotificationRecipient> recipients = new HashSet<>();

    public void addRecipient(RuleNotificationRecipient r) {
        recipients.add(r);
        r.setNotification(this);
    }

    public void addRecipient(Contact c) {
        RuleNotificationRecipient recipient = new RuleNotificationRecipient();
        recipient.setId(new RuleNotificationRecipientId());
        recipient.setContact(c);
        addRecipient(recipient);
    }

    public void removeRecipient(RuleNotificationRecipient r) {
        recipients.remove(r);
        r.setNotification(null);
    }
}