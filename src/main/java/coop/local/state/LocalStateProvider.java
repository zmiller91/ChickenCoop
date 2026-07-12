package coop.local.state;

import coop.device.protocol.event.RemoteManualCommandEvent;
import coop.local.listener.EventProcessor;
import coop.shared.database.table.Coop;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.events.HubEvent;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Getter
@Log4j2
public abstract class LocalStateProvider extends StateProvider {

    private CoopState config = null;

    public abstract void init();
    public abstract void refreshState();
    public abstract void save(HubEvent event);

    @Override
    public void put(CoopState coopState) {
        this.config = coopState;
    }

    /**
     * Both database mode and mqtt mode run this in-process, on the Pi, alongside EventProcessor - so a command
     * can be dispatched directly without any network hop. (In mqtt mode, this is only reachable if something
     * inside this same process calls it directly; commands relayed from the cloud instead arrive via
     * CommandSubscription, which calls EventProcessor.receiveSyntheticEvent the same way.)
     */
    @Override
    public void sendCommand(Coop coop, String componentId, String actionKey, Map<String, String> params) {

        if (config == null) {
            log.warn("Cannot send command, no state loaded yet.");
            return;
        }

        ComponentState component = config.getComponents()
                .stream()
                .filter(c -> componentId.equals(c.getComponentId()))
                .findFirst()
                .orElse(null);

        EventProcessor.receiveSyntheticEvent(config, component, new RemoteManualCommandEvent(actionKey, params));
    }
}
