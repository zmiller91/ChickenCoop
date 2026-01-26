package coop.shared.database.table;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "contact")
@Data
public class Contact implements AuthorizerScopedTable{

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(name = "CONTACT_ID", length = 32, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COOP_ID", nullable = false)
    private Coop coop;

    @Column(name = "DISPLAY_NAME", length = 128, nullable = false)
    private String displayName;

    @Column(name = "EMAIL", length = 256)
    private String email;

    @Column(name = "PHONE", length = 32)
    private String phone;

    @Override
    public User getUser() {
        return coop.getUser();
    }

    @Override
    public Pi getPi() {
        return coop.getPi();
    }
}
