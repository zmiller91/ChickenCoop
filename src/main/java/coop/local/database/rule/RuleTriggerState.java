package coop.local.database.rule;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "rule_trigger_state")
public class RuleTriggerState {

    @Id
    @Column(name="rule_id")
    private String ruleId;

    @Column(name="trigger_state")
    @Enumerated(EnumType.STRING)
    private TriggerState triggerState;

    @Column(name = "trigger_state_dt")
    private long triggerStateDt;
}
