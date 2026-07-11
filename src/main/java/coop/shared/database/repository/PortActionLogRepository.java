package coop.shared.database.repository;

import coop.shared.database.table.component.Component;
import coop.shared.database.table.component.PortActionLogEntry;
import coop.shared.database.table.component.PortActionStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class PortActionLogRepository extends AuthorizerScopedRepository<PortActionLogEntry> {

    @Override
    protected Class<PortActionLogEntry> getObjClass() {
        return PortActionLogEntry.class;
    }

    public List<PortActionLogEntry> findRecent(Component component, int portIndex, int limit) {
        return this.query(
                "FROM PortActionLogEntry WHERE component = :component AND portIndex = :portIndex ORDER BY createdAt DESC",
                PortActionLogEntry.class)
                .setParameter("component", component)
                .setParameter("portIndex", portIndex)
                .setMaxResults(limit)
                .list();
    }

    /**
     * The most recent device-confirmed (COMPLETE) entry per port, i.e. each port's last known actual on/off
     * state. Ports with no confirmed entry yet (never reported, or only REQUESTED/FAILED/CANCELLED so far)
     * simply won't appear in the result.
     */
    public List<PortActionLogEntry> findLatestComplete(Component component) {
        return this.query(
                "FROM PortActionLogEntry p1 WHERE p1.component = :component AND p1.status = :status " +
                "AND p1.createdAt = (SELECT MAX(p2.createdAt) FROM PortActionLogEntry p2 " +
                "WHERE p2.component = :component AND p2.portIndex = p1.portIndex AND p2.status = :status)",
                PortActionLogEntry.class)
                .setParameter("component", component)
                .setParameter("status", PortActionStatus.COMPLETE)
                .list();
    }
}
