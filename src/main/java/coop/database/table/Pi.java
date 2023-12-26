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
    private String id;

    @Column(name = "AWS_IOT_THING_ID")
    private String awsIotThingId;

    @Column(name = "CLIENT_ID")
    private String clientId;
}
