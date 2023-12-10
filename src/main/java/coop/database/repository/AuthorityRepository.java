package coop.database.repository;

import coop.database.table.Authority;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorityRepository extends GenericRepository<Authority> {

    @Override
    protected Class<Authority> getObjClass() {
        return Authority.class;
    }
}
