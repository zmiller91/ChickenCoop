package coop.shared.database.repository;

import coop.shared.database.table.ComponentSerial;
import coop.shared.database.table.CoopComponent;
import coop.shared.database.table.Coop;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class ComponentRepository extends AuthorizerScopedRepository<CoopComponent>  {
    @Override
    protected Class<CoopComponent> getObjClass() {
        return CoopComponent.class;
    }

    public List<CoopComponent> findByCoop(Coop coop) {
        return this.query("FROM CoopComponent WHERE coop = :coop", CoopComponent.class)
                .setParameter("coop", coop)
                .list();
    }

    public CoopComponent findBySerialNumber(Coop coop, ComponentSerial serial) {
        return this.query(
            """
                FROM CoopComponent cc 
                WHERE coop = :coop
                AND serial = :serial
                """, CoopComponent.class)
                .setParameter("coop", coop)
                .setParameter("serial", serial)
                .list()
                .stream()
                .findFirst()
                .orElse(null);

    }

}
