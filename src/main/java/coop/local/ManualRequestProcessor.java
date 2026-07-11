package coop.local;

import coop.device.Actuator;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.ManualOverrideEvent;
import coop.device.protocol.event.ManualRequestEvent;
import coop.device.protocol.event.RemoteManualCommandEvent;
import coop.local.listener.EventListener;
import coop.local.scheduler.Scheduler;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ManualRequestProcessor implements EventListener {


    private final LocalStateProvider provider;
    private final Scheduler scheduler;

    @Override
    public List<Class<? extends Event>> listenForClasses() {
        return List.of(ManualRequestEvent.class, ManualOverrideEvent.class, RemoteManualCommandEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {

        if(payload.getEvent() instanceof ManualRequestEvent event) {
            processRequest(event);
        }

        if(payload.getEvent() instanceof ManualOverrideEvent event) {
            processOverride(event);
        }

        if(payload.getEvent() instanceof RemoteManualCommandEvent event) {
            processRemoteCommand(payload.getComponent(), event);
        }
    }

    private void processRequest(ManualRequestEvent event) {

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

        if (component.getDeviceType().getDevice() instanceof Actuator actuator) {

            // Create the downlink frame from the request
            DownlinkFrame requestedFrame = actuator.manualRequest(event, component.getSerialNumber(), component.getConfig(), component.getPortConfig());
            if(requestedFrame != null) {
                scheduler.createSupersedingExisting(component, actuator, requestedFrame);
            }
        }

        System.out.println("Manual request received.");
    }

    private void processOverride(ManualOverrideEvent event) {
        System.out.println("Manual override recieved.");
    }

    /**
     * Handles a one-shot manual command relayed from the cloud (or, in free-tier/database mode, the local
     * webserver running in the same process). Params are already fully resolved server-side, so this goes
     * straight to createCommand rather than the physical-uplink manualRequest(...) path.
     */
    private void processRemoteCommand(ComponentState component, RemoteManualCommandEvent event) {

        if(component == null || component.getDeviceType() == null) {
            return;
        }

        if (component.getDeviceType().getDevice() instanceof Actuator actuator) {
            DownlinkFrame frame = actuator.createCommand(component.getSerialNumber(), event.getActionKey(), event.getParams());
            if(frame != null) {
                scheduler.createSupersedingExisting(component, actuator, frame);
            }
        }
    }

}
