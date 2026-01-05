package coop.shared.database.table.rule;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Data
@Table(name = "rule_trigger_component")
public class ComponentRuleTrigger implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "rule_trigger_component_id")
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @ManyToOne
    @JoinColumn(name = "component_id")
    private Component component;

    @Column
    private String metric;

    @Column
    private double threshold;

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
