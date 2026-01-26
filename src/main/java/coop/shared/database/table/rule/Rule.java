package coop.shared.database.table.rule;

import coop.shared.database.table.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rule")
public class Rule implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "rule_id")
    private String id;

    @ManyToOne
    @JoinColumn(name="COOP_ID")
    private Coop coop;

    @Column(name="name")
    private String name;

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComponentRuleTrigger> componentTriggers = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduledRuleTrigger> scheduleTriggers = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleAction> actions = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleNotification> notifications = new ArrayList<>();

    @Override
    public User getUser() {
        return coop.getUser();
    }

    @Override
    public Pi getPi() {
        return coop.getPi();
    }

    public void addScheduledTrigger(ScheduledRuleTrigger t) {
        scheduleTriggers.add(t);
        t.setRule(this);
    }

    public void removeScheduledTrigger(ScheduledRuleTrigger t) {
        scheduleTriggers.remove(t);
        t.setRule(null);
    }

    public void addComponentTrigger(ComponentRuleTrigger t) {
        componentTriggers.add(t);
        t.setRule(this);
    }

    public void removeComponentTrigger(ComponentRuleTrigger t) {
        componentTriggers.remove(t);
        t.setRule(null);
    }

    public void addAction(RuleAction a) {
        actions.add(a);
        a.setRule(this);
    }

    public void removeAction(RuleAction a) {
        actions.remove(a);
        a.setRule(null);
    }

    public void addNotification(RuleNotification n) {
        notifications.add(n);
        n.setRule(this);
    }

    public void removeNotification(RuleNotification n) {
        notifications.remove(n);
        n.setRule(null);
    }
}