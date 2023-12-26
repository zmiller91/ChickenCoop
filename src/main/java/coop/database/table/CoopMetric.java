package coop.database.table;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "metrics")
public class CoopMetric implements Serializable {

    @Id
    @Column(name="DT")
    private long dt;

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
    private long value;

}
