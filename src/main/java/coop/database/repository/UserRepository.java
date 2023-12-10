package coop.database.repository;

import coop.database.table.User;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class UserRepository extends GenericRepository<User> {


    @Autowired
    SessionFactory sessionFactory;

    public List<User> list() {
        return this.query("from User", User.class).list();
    }

    public User findByUsername(String username) {
        return sessionFactory
                .getCurrentSession()
                .createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    protected Class<User> getObjClass() {
        return User.class;
    }
}
