package coop.shared.database.repository;

import coop.shared.database.table.AreaComponentPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class AreaComponentPortRepository extends GenericRepository<AreaComponentPort> {

    @Override
    protected Class<AreaComponentPort> getObjClass() {
        return AreaComponentPort.class;
    }

    public List<AreaComponentPort> findByComponentAndPort(String componentId, int portIndex) {
        return this.query("FROM AreaComponentPort WHERE id.componentId = :componentId AND id.portIndex = :portIndex",
                        AreaComponentPort.class)
                .setParameter("componentId", componentId)
                .setParameter("portIndex", portIndex)
                .list();
    }

    /**
     * Replaces the full set of areas a port belongs to in one call - same rationale as
     * AreaComponentRepository.deleteByComponent.
     */
    public void deleteByComponentAndPort(String componentId, int portIndex) {
        this.query("DELETE FROM AreaComponentPort WHERE id.componentId = :componentId AND id.portIndex = :portIndex",
                        AreaComponentPort.class)
                .setParameter("componentId", componentId)
                .setParameter("portIndex", portIndex)
                .executeUpdate();
    }
}
