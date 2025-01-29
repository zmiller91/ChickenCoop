package coop.shared.pi;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.table.ComponentConfig;
import coop.shared.database.table.Coop;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StateFactory {

    @Autowired
    private ComponentRepository componentRepository;

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
                    state.setComponentType(component.getSerial().getComponentType());
                    state.setSerialNumber(component.getSerial().getSerialNumber());
                    return state;

                }).toList();


        CoopState config = new CoopState();
        config.setCoopId(coop.getId());
        config.setComponents(components);
        config.setAwsIotThingId(coop.getPi().getAwsIotThingId());
        return config;

    }

}
