package coop.shared.database.repository;

import coop.shared.database.table.Pi;
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

    public Pi findByClientId(String clientId) {
        return this.query("FROM Pi WHERE clientId = :clientId", Pi.class)
                .setParameter("clientId", clientId)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Pi findByThumbprint(String thumbprint) {
        return this.query("FROM Pi WHERE thumbprint = :thumbprint", Pi.class)
                .setParameter("thumbprint", thumbprint)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }
}
