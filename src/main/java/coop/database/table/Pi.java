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
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "pi_id")
    public String id;

    @ManyToOne
    @JoinColumn(name="user_id")
    User user;

    public String name;

    @OneToOne
    @JoinColumn(name="public_key_id")
    public PublicKey publicKey;
}
