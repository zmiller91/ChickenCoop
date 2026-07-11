package coop.shared.pi;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.table.Coop;
import coop.shared.pi.config.CoopState;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public abstract class StateProvider {

    @Autowired
    private ComponentRepository componentRepository;

    public abstract void put(CoopState coopState);

    /**
     * Delivers a one-shot, non-durable command (e.g. a manual valve toggle) to the coop's device. Unlike
     * put(CoopState), this is not part of the coop's durable/declarative state - it's an event that's either
     * acted on immediately or not at all.
     */
    public abstract void sendCommand(Coop coop, String componentId, String actionKey, Map<String, String> params);
}
