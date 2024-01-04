package coop.shared.database.repository;

import coop.shared.database.table.ComponentConfig;
import coop.shared.database.table.CoopComponent;
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

    public ComponentConfig findByKey(CoopComponent component, String key) {
        return this.query("FROM ComponentConfig WHERE key = :key and component = :component", ComponentConfig.class)
                .setParameter("key", key)
                .setParameter("component", component)
                .list()
                .stream()
                .findFirst()
                .orElse(null);

    }

    public ComponentConfig save(CoopComponent component, String key, String value) {
        ComponentConfig config = findByKey(component, key);
        if (config == null) {
            config = new ComponentConfig();
            config.setComponent(component);
            config.setKey(key);
        }

        config.setValue(value);
        this.persist(config);
        return config;
    }
}
