package coop.database.repository;

import coop.database.table.Pi;
import coop.database.table.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class PiRepository extends GenericRepository<Pi> {

    public Pi create(User user, String name) {
        Pi pi = new Pi();
        pi.setUser(user);
        pi.setName(name);
        this.persist(pi);
        return pi;
    }

    public List<Pi> list(User user) {
        return this.query("FROM Pi WHERE user = :user", Pi.class)
                .setParameter("user", user)
                .list();
    }

    @Override
    protected Class<Pi> getObjClass() {
        return Pi.class;
    }
}
