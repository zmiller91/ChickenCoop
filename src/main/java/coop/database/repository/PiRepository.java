package coop.database.repository;

import coop.database.table.Pi;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Repository
@EnableTransactionManagement
@Transactional
public class PiRepository extends GenericRepository<Pi> {
    @Override
    protected Class<Pi> getObjClass() {
        return Pi.class;
    }
}
