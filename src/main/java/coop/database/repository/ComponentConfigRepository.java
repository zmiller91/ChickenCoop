package coop.database.repository;

import coop.database.table.ComponentConfig;
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
