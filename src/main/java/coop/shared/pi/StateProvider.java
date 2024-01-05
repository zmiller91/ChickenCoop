package coop.shared.pi;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.table.ComponentConfig;
import coop.shared.database.table.Coop;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StateProvider {

    @Autowired
    private ComponentRepository componentRepository;

    public abstract void put(Coop coop, CoopState coopState);

    public CoopState forCoop(Coop coop) {

        if(coop == null) {
            return new CoopState();
        }

        List<ComponentState> components = componentRepository
                .findByCoop(coop)
                .stream()
                .map( component -> {

                    Map<String, String> config = new HashMap<>();
                    for (ComponentConfig c : component.getConfig()) {
                        config.put(c.getKey(), c.getValue());
                    }

                    ComponentState state = new ComponentState();
                    state.setComponentId(component.getComponentId());
                    state.setConfig(config);
                    return state;

                }).toList();


        CoopState config = new CoopState();
        config.setCoopId(coop.getId());
        config.setComponents(components);
        return config;

    }
}
