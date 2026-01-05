package coop.shared.database.table.rule;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rule_action")
public class RuleAction implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "rule_action_id")
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @ManyToOne
    @JoinColumn(name = "component_id")
    private Component component;

    @Column(name="action_key")
    private String actionKey;

    @OneToMany(
            mappedBy = "ruleAction",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<RuleActionParam> params = new HashSet<>();

    @Override
    public User getUser() {
        return rule.getUser();
    }

    @Override
    public Pi getPi() {
        return rule.getPi();
    }

    public java.util.Map<String, String> getParamsMap() {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        for (RuleActionParam p : params) {
            out.put(p.getKey(), p.getValue());
        }
        return out;
    }

    public void setParamsMap(java.util.Map<String, String> map) {
        params.clear();
        if (map == null) return;

        for (var e : map.entrySet()) {
            params.add(new RuleActionParam(this, e.getKey(), e.getValue()));
        }
    }

}
