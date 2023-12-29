package coop.database.table;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "components")
public class CoopComponent implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "COMPONENT_ID")
    private String componentId;

    @OneToOne
    @JoinColumn(name="SERIAL_NUMBER")
    private ComponentSerial serial;

    @OneToOne
    @JoinColumn(name="COOP_ID")
    private Coop coop;

    @Column(name = "name")
    private String name;

    @OneToMany
    @JoinColumn(name = "COMPONENT_ID")
    private List<ComponentConfig> config;

    @Override
    public User getUser() {
        return coop.getUser();
    }

    @Override
    public Pi getPi() {
        return coop.getPi();
    }
}
