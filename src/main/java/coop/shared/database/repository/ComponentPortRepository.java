package coop.shared.database.repository;

import coop.shared.database.table.component.Component;
import coop.shared.database.table.component.ComponentPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@EnableTransactionManagement
@Transactional
public class ComponentPortRepository extends AuthorizerScopedRepository<ComponentPort> {

    @Override
    protected Class<ComponentPort> getObjClass() {
        return ComponentPort.class;
    }

    public List<ComponentPort> findByComponent(Component component) {
        return this.query("FROM ComponentPort WHERE component = :component ORDER BY portIndex", ComponentPort.class)
                .setParameter("component", component)
                .list();
    }

    public ComponentPort findByIndex(Component component, int portIndex) {
        return this.query("FROM ComponentPort WHERE component = :component AND portIndex = :portIndex", ComponentPort.class)
                .setParameter("component", component)
                .setParameter("portIndex", portIndex)
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public ComponentPort save(Component component, int portIndex, String name) {
        ComponentPort port = findByIndex(component, portIndex);
        if (port == null) {
            port = new ComponentPort();
            port.setComponent(component);
            port.setPortIndex(portIndex);
        }

        port.setName(name);
        this.persist(port);
        return port;
    }
}
