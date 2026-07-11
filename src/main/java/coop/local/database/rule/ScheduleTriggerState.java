package coop.local.database.rule;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "schedule_trigger_state")
public class ScheduleTriggerState {

    @Id
    @Column(name = "schedule_trigger_id")
    private String scheduleTriggerId;

    @Column(name = "last_fired_dt")
    private long lastFiredDt;
}
