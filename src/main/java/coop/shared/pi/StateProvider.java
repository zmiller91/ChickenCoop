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

    public abstract void put(CoopState coopState);
}
