package coop.database.repository;

import coop.database.table.ComponentSerial;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Repository
@EnableTransactionManagement
@Transactional
public class ComponentSerialRepository extends GenericRepository<ComponentSerial> {
    @Override
    protected Class<ComponentSerial> getObjClass() {
        return ComponentSerial.class;
    }
}
