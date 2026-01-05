package coop.shared.database.table.rule;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Entity
@Table(name = "rule_trigger_schedule")
public class ScheduledRuleTrigger implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "rule_trigger_schedule_id")
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    @Column
    @Enumerated(EnumType.STRING)
    private ScheduleFrequency frequency;

    @Column
    private int hour;

    @Column
    private int minute;

    @Column
    private int gap;

    @Override
    public User getUser() {
        return rule.getUser();
    }

    @Override
    public Pi getPi() {
        return rule.getPi();
    }
}
