package coop.shared.database.table.rule;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "rule_trigger_time")
public class RuleTimeTrigger implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "rule_trigger_time_id")
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Column
    private int hour;

    @Column
    private int minute;

    @Column
    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Override
    public User getUser() {
        return rule.getUser();
    }

    @Override
    public Pi getPi() {
        return rule.getPi();
    }
}
