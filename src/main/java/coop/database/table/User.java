package coop.database.table;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "user_id")
    public String id;

    public String username;

    public String password;

    public boolean enabled;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    public List<Authority> authorities;
}
