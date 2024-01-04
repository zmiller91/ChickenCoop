package coop.shared.database.repository;

import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class CoopRepository extends AuthorizerScopedRepository<Coop> {

    public Coop create(User user, String name, Pi pi) {
        Coop coop = new Coop();
        coop.setPi(pi);
        coop.setName(name);
        coop.setUser(user);
        this.persist(coop);
        return coop;
    }

    public List<Coop> list(User user) {
        return this.query("FROM Coop WHERE user = :user", Coop.class)
                .setParameter("user", user)
                .list();
    }

    @Override
    protected Class<Coop> getObjClass() {
        return Coop.class;
    }
}
