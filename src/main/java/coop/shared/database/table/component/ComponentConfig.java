package coop.shared.database.table.component;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "component_config")
public class ComponentConfig implements AuthorizerScopedTable, Serializable {

    @Id
    @OneToOne
    @JoinColumn(name="COMPONENT_ID")
    private Component component;

    @Id
    @Column(name="CONFIG_KEY")
    private String key;

    @Column(name="CONFIG_VALUE")
    private String value;

    @Override
    public User getUser() {
        return component.getUser();
    }

    @Override
    public Pi getPi() {
        return component.getPi();
    }
}
