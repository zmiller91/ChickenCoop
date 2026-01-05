package coop.shared.database.repository;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Repository
@EnableTransactionManagement
@Transactional
public abstract class GenericRepository<T> {

    @Autowired
    protected SessionFactory sessionFactory;

    public void persist(T obj) {
        sessionFactory.getCurrentSession().persist(obj);
    }

    public void refresh(T obj) {
        sessionFactory.getCurrentSession().refresh(obj);
    }

    protected Query<T> query(String query, Class<T> clazz) {
       return sessionFactory.getCurrentSession().createQuery(query, clazz);
    }

    public void flush() {
        sessionFactory.getCurrentSession().flush();
    }

    public void delete(T obj) {
        sessionFactory.getCurrentSession().remove(obj);
    }


    public T findById(Serializable id) {
        return sessionFactory.getCurrentSession().get(getObjClass(), id);
    }

    protected abstract Class<T> getObjClass();

}
