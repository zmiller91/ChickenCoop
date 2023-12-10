package coop.database.table;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "authorities")
public class Authority {

    @Id
    @Column(name = "authority_id")
    long id;

    @ManyToOne
    @JoinColumn(name="user_id")
    User user;

    String authority;


}
