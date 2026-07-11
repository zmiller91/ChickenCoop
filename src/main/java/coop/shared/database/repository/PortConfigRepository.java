package coop.shared.database.repository;

import coop.shared.database.table.component.Component;
import coop.shared.database.table.component.PortConfig;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class PortConfigRepository extends AuthorizerScopedRepository<PortConfig> {

    @Override
    protected Class<PortConfig> getObjClass() {
        return PortConfig.class;
    }

    public List<PortConfig> findByComponent(Component component) {
        return this.query("FROM PortConfig WHERE component = :component", PortConfig.class)
                .setParameter("component", component)
                .list();
    }

    public List<PortConfig> findByPort(Component component, int portIndex) {
        return this.query("FROM PortConfig WHERE component = :component AND portIndex = :portIndex", PortConfig.class)
                .setParameter("component", component)
                .setParameter("portIndex", portIndex)
                .list();
    }

    public PortConfig findByKey(Component component, int portIndex, String key) {
        return this.query("FROM PortConfig WHERE component = :component AND portIndex = :portIndex AND key = :key", PortConfig.class)
                .setParameter("component", component)
                .setParameter("portIndex", portIndex)
                .setParameter("key", key)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public PortConfig save(Component component, int portIndex, String key, String value) {
        PortConfig config = findByKey(component, portIndex, key);
        if (config == null) {
            config = new PortConfig();
            config.setComponent(component);
            config.setPortIndex(portIndex);
            config.setKey(key);
        }

        config.setValue(value);
        this.persist(config);
        return config;
    }
}
