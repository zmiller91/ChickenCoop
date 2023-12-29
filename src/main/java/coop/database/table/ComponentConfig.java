package coop.database.table;

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
    private CoopComponent component;

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
