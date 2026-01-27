package coop.shared.database.repository;

import coop.shared.database.table.Coop;
import coop.shared.database.table.inbox.InboxMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InboxMessageRepository extends AuthorizerScopedRepository<InboxMessage> {

    @Override
    protected Class<InboxMessage> getObjClass() {
        return InboxMessage.class;
    }

    public InboxMessage findByCoopAndId(Coop coop, String id) {
        return this.query(
                        "FROM InboxMessage m " +
                                "WHERE m.coop = :coop " +
                                "  AND m.id = :id",
                        InboxMessage.class
                )
                .setParameter("coop", coop)
                .setParameter("id", id)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<InboxMessage> findByCoop(Coop coop, int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.min(Math.max(1, pageSize), 200);
        int offset = safePage * safePageSize;

        return this.query(
                        "FROM InboxMessage m " +
                                "WHERE m.coop = :coop " +
                                "  AND m.deletedTs IS NULL " +
                                "ORDER BY m.createdTs DESC",
                        InboxMessage.class
                )
                .setParameter("coop", coop)
                .setFirstResult(offset)
                .setMaxResults(safePageSize)
                .list();
    }

    public long countUnreadByCoop(Coop coop) {
        return sessionFactory.getCurrentSession().createQuery(
                        "SELECT COUNT(m) FROM InboxMessage m " +
                                "WHERE m.coop = :coop " +
                                "  AND m.readTs IS NULL " +
                                "  AND m.archivedTs IS NULL " +

                                "  AND m.deletedTs IS NULL",
                        Long.class
                )
                .setParameter("coop", coop)
                .uniqueResult();
    }
}
