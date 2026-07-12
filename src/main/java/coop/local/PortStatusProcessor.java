package coop.local;

import coop.device.Actuator;
import coop.device.PortCommand;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.StatusEvent;
import coop.local.listener.EventListener;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.events.PortActionHubEvent;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Handles the port-state subset of device-reported status broadcasts (StatusEvent) - independent of any
 * specific command's lifecycle. This is what actually confirms a port's physical on/off state, unlike
 * CommandCompleteEvent (which only signals a downlink frame's own lifecycle ended, not that the state it
 * requested is now true - see the TURN_ON-then-TURN_OFF interleaving issue that motivated this).
 *
 * StatusEvent is multi-purpose (its "type" can be anything a device wants to report, e.g. a battery level,
 * not just port state) - this listener only acts when Actuator.describePortStatus recognizes the type as a
 * port state change and ignores everything else. A future non-port status type would get its own listener
 * on StatusEvent rather than being added here.
 *
 * describePortStatus can describe multiple ports at once (e.g. a combined all-ports state broadcast) - this
 * stays tier-agnostic (works the same whether provider.save() persists directly or forwards over MQTT), so
 * it doesn't dedupe against last-known state itself; that happens wherever the event is actually persisted
 * (DatabaseStateProvider/PortActionProcessor), since only those layers have DB access in every deployment
 * tier.
 */
@AllArgsConstructor
public class PortStatusProcessor implements EventListener {

    private final LocalStateProvider provider;

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return List.of(StatusEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {
        if(payload.getEvent() instanceof StatusEvent event) {
            process(event);
        }
    }

    private void process(StatusEvent event) {

        if(provider.getConfig() == null) {
            return;
        }

        ComponentState component = provider.getConfig()
                .getComponents()
                .stream()
                .filter(c -> c.getSerialNumber() != null)
                .filter(c -> c.getSerialNumber().equals(event.getSerialNumber()))
                .findFirst()
                .orElse(null);

        if(component == null || component.getDeviceType() == null) {
            return;
        }

        if(!(component.getDeviceType().getDevice() instanceof Actuator actuator)) {
            return;
        }

        List<PortCommand> described = actuator.describePortStatus(event);
        if(described == null || described.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        for(PortCommand command : described) {
            PortActionHubEvent portEvent = new PortActionHubEvent(
                    component.getComponentId(), command.portIndex(), command.actionKey(), null, "COMPLETE");
            portEvent.setDt(now);
            portEvent.setCoopId(provider.getConfig().getCoopId());
            provider.save(portEvent);
        }
    }
}
