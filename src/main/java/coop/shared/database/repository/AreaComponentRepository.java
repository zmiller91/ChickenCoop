package coop.shared.database.repository;

import coop.shared.database.table.AreaComponent;
import coop.shared.database.table.component.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class AreaComponentRepository extends GenericRepository<AreaComponent> {

    @Override
    protected Class<AreaComponent> getObjClass() {
        return AreaComponent.class;
    }

    public List<AreaComponent> findByComponent(Component component) {
        return this.query("FROM AreaComponent WHERE component = :component", AreaComponent.class)
                .setParameter("component", component)
                .list();
    }

    /**
     * Replaces the full set of areas a component belongs to in one call - simplest operation for a
     * multi-select UI to drive (send the whole selected list, not incremental add/remove calls).
     */
    public void deleteByComponent(Component component) {
        // Typed queries (this.query(..., Class)) can't be DML in Hibernate - "Update/delete queries
        // cannot be typed" - so this goes straight through the session instead, same as the other
        // executeUpdate() call sites in this codebase (e.g. JobRepository.updateStatus).
        sessionFactory.getCurrentSession()
                .createQuery("DELETE FROM AreaComponent WHERE component = :component")
                .setParameter("component", component)
                .executeUpdate();
    }
}
