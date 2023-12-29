package coop.database.repository;

import coop.database.table.CoopComponent;
import coop.database.table.Coop;
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
        return this.query("FROM Component WHERE coop = :coop", CoopComponent.class)
                .setParameter("coop", coop)
                .list();
    }

}
