package coop.shared.pi;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.pi.config.CoopState;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class StateProvider {

    @Autowired
    private ComponentRepository componentRepository;

    public abstract void put(CoopState coopState);
}
