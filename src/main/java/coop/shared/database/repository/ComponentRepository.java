package coop.shared.database.repository;

import coop.shared.database.table.component.ComponentSerial;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.Coop;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class ComponentRepository extends AuthorizerScopedRepository<Component>  {
    @Override
    protected Class<Component> getObjClass() {
        return Component.class;
    }

    public List<Component> findByCoop(Coop coop) {
        return this.query("FROM Component WHERE coop = :coop", Component.class)
                .setParameter("coop", coop)
                .list();
    }

    public Component findBySerialNumber(Coop coop, ComponentSerial serial) {
        return this.query(
            """
                FROM Component cc 
                WHERE coop = :coop
                AND serial = :serial
                """, Component.class)
                .setParameter("coop", coop)
                .setParameter("serial", serial)
                .list()
                .stream()
                .findFirst()
                .orElse(null);

    }

}
