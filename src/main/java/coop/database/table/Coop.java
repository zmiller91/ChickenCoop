package coop.database.table;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "coops")
public class Coop implements UserScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "coop_id")
    public String id;

    @OneToOne
    @JoinColumn(name="user_id")
    public User user;

    @OneToOne
    @JoinColumn(name="pi_id")
    public Pi pi;

    public String name;
}
