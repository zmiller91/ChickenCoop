package coop.shared.database.repository;

import coop.shared.database.table.ComponentConfig;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Repository
@EnableTransactionManagement
@Transactional
public class ComponentConfigRepository extends AuthorizerScopedRepository<ComponentConfig>  {
    @Override
    protected Class<ComponentConfig> getObjClass() {
        return ComponentConfig.class;
    }
}
