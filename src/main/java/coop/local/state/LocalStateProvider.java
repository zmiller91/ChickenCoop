package coop.local.state;

import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.events.HubEvent;
import lombok.Getter;

@Getter
public abstract class LocalStateProvider extends StateProvider {

    private CoopState config = null;

    public abstract void init();
    public abstract void refreshState();
    public abstract void save(HubEvent event);

    @Override
    public void put(CoopState coopState) {
        this.config = coopState;
    }
}
