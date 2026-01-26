package coop.shared.database.repository;

import coop.shared.database.table.Contact;
import coop.shared.database.table.Coop;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
@EnableTransactionManagement
@Transactional
public class ContactRepository extends AuthorizerScopedRepository<Contact>{

    @Override
    protected Class<Contact> getObjClass() {
        return Contact.class;
    }


    public List<Contact> findByCoop(Coop coop) {
        return this.query("FROM Contact WHERE coop = :coop", Contact.class)
                .setParameter("coop", coop)
                .list();
    }

    public Contact findByIdAndCoop(Coop coop, String id) {
        return this.query("FROM Contact WHERE coop = :coop AND id = :id", Contact.class)
                .setParameter("coop", coop)
                .setParameter("id", id)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
