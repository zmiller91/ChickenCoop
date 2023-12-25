package coop.database.table;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "pis")
public class Pi {

    @Id
    @Column(name = "PI_ID")
    public String id;

    @Column(name = "AWS_IOT_THING_ID")
    String awsIotThingId;
}
