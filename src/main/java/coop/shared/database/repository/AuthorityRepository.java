package coop.shared.database.repository;

import coop.shared.database.table.Authority;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorityRepository extends GenericRepository<Authority> {

    @Override
    protected Class<Authority> getObjClass() {
        return Authority.class;
    }
}
