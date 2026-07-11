package coop.shared.database.table.component;

import coop.shared.database.table.AuthorizerScopedTable;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * A single entry in a port's action history - a command was requested (manually or by a rule), or the
 * device reported back that it completed, failed, or the request was cancelled (superseded by a newer one).
 */
@Getter
@Setter
@Entity
@Table(name = "port_action_log")
public class PortActionLogEntry implements AuthorizerScopedTable {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @Column(columnDefinition = "CHAR(32)", name = "ID")
    private String id;

    @OneToOne
    @JoinColumn(name = "COMPONENT_ID")
    private Component component;

    @Column(name = "PORT_INDEX")
    private int portIndex;

    @Column(name = "ACTION_KEY")
    private String actionKey;

    @Column(name = "SOURCE")
    @Enumerated(EnumType.STRING)
    private PortActionSource source;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private PortActionStatus status;

    @Column(name = "CREATED_AT")
    private long createdAt;

    @Override
    public User getUser() {
        return component.getUser();
    }

    @Override
    public Pi getPi() {
        return component.getPi();
    }
}
