package coop.database.table;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "metrics")
public class CoopMetric implements Serializable, AuthorizerScopedTable {

    @Id
    @Column(name="DT")
    private long dt;

    @Column(name="YEAR")
    private int year;

    @Column(name="MONTH")
    private int month;

    @Column(name="DAY")
    private int day;

    @Column(name="HOUR")
    private int hour;

    @Id
    @OneToOne
    @JoinColumn(name="COOP_ID")
    private Coop coop;

    @Id
    @Column(name="COMPONENT_ID")
    private String componentId;

    @Id
    @Column(name="METRIC")
    private String metric;

    @Column(name="VALUE")
    private Double value;

    @Override
    public User getUser() {
        return coop.getUser();
    }

    @Override
    public Pi getPi() {
        return coop.getPi();
    }
}
