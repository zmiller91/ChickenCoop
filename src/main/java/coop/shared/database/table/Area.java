package coop.shared.database.table;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "area")
@Data
public class Area implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(name = "AREA_ID", length = 32, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COOP_ID", nullable = false)
    private Coop coop;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "PARENT_AREA_ID")
    private Area parent;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", length = 32, nullable = false)
    private AreaType type;

    @Override
    public User getUser() {
        return coop.getUser();
    }

    @Override
    public Pi getPi() {
        return coop.getPi();
    }
}
