package coop.shared.database.table.rule;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@Embeddable
public class RuleNotificationRecipientId implements Serializable {

    @Column(name = "RULE_NOTIFICATION_ID", nullable = false)
    private String ruleNotificationId;

    @Column(name = "CONTACT_ID", nullable = false)
    private String contactId;
}
