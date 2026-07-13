package coop.shared.database.repository;

import coop.shared.database.table.Area;
import coop.shared.database.table.Coop;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class AreaRepository extends AuthorizerScopedRepository<Area> {

    @Override
    protected Class<Area> getObjClass() {
        return Area.class;
    }

    public List<Area> findByCoop(Coop coop) {
        return this.query("FROM Area WHERE coop = :coop", Area.class)
                .setParameter("coop", coop)
                .list();
    }

    public Area findByIdAndCoop(Coop coop, String id) {
        return this.query("FROM Area WHERE coop = :coop AND id = :id", Area.class)
                .setParameter("coop", coop)
                .setParameter("id", id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<Area> findByIdsAndCoop(Coop coop, List<String> ids) {
        return this.query("FROM Area WHERE coop = :coop AND id IN :ids", Area.class)
                .setParameter("coop", coop)
                .setParameter("ids", ids)
                .list();
    }

    public List<Area> findByParent(Area parent) {
        return this.query("FROM Area WHERE parent = :parent", Area.class)
                .setParameter("parent", parent)
                .list();
    }
}
