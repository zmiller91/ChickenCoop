package coop.shared.database.table.rule;

import coop.shared.database.table.Contact;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "rule_notification_recipient")
public class RuleNotificationRecipient {

    @EmbeddedId
    private RuleNotificationRecipientId id;

    @MapsId("ruleNotificationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RULE_NOTIFICATION_ID", nullable = false)
    private RuleNotification notification;

    @MapsId("contactId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CONTACT_ID", nullable = false)
    private Contact contact;
}