package coop.shared.database.table.rule;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * A record of a single moment a rule was satisfied and fired, independent of whether its actions were ever
 * confirmed by the physical device (see PortActionLogEntry for that) or whether any notification was
 * configured for it.
 */
@Getter
@Setter
@Entity
@Table(name = "rule_execution_log")
public class RuleExecutionLogEntry implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "ID")
    private String id;

    @ManyToOne
    @JoinColumn(name = "RULE_ID")
    private Rule rule;

    @Column(name = "CREATED_AT")
    private long createdAt;

    @Override
    public User getUser() {
        return rule.getUser();
    }

    @Override
    public Pi getPi() {
        return rule.getPi();
    }
}
