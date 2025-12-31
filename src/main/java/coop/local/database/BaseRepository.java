package coop.local.database;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = "piTransactionManager")
public class BaseRepository {

    @Autowired
    @Qualifier("piSessionFactory")
    protected SessionFactory sessionFactory;

    public void persist(Object obj) {
        sessionFactory.getCurrentSession().persist(obj);
    }

    public void flush() {
        sessionFactory.getCurrentSession().flush();
    }

    public void refresh(Object obj) {
        sessionFactory.getCurrentSession().refresh(obj);
    }

}
