package coop.shared.database.table.rule;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "rule_action_param")
public class RuleActionParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="rule_action_param_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_action_id")
    private RuleAction ruleAction;

    @Column(name = "param_key", length = 64, nullable = false)
    private String key;

    @Column(name = "param_value", length = 255)
    private String value;

    public RuleActionParam(RuleAction action, String key, String value) {
        this.ruleAction = action;
        this.key = key;
        this.value = value;
    }
}