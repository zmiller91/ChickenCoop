package coop.shared.database.table;

import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "CERT_THUMBPRINT")
    private String thumbprint;
}
