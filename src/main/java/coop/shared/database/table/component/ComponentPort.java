package coop.shared.database.table.component;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

/**
 * A user-facing display name for one of a component's 8 ports (e.g. port 3 -> "Front garden").
 * Purely cosmetic - the wire protocol still addresses ports by index, this is only joined in for display.
 */
@Getter
@Setter
@Entity
@Table(name = "component_ports")
public class ComponentPort implements AuthorizerScopedTable, Serializable {

    @Id
    @OneToOne
    @JoinColumn(name = "COMPONENT_ID")
    private Component component;

    @Id
    @Column(name = "PORT_INDEX")
    private int portIndex;

    @Column(name = "NAME")
    private String name;

    @Override
    public User getUser() {
        return component.getUser();
    }

    @Override
    public Pi getPi() {
        return component.getPi();
    }
}
