package coop.local;

import coop.device.Actuator;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.ManualOverrideEvent;
import coop.device.protocol.event.ManualRequestEvent;
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
        return List.of(ManualRequestEvent.class, ManualOverrideEvent.class);
    }

    @Override
    public void receive(EventPayload payload) {

        if(payload.getEvent() instanceof ManualRequestEvent event) {
            processRequest(event);
        }

        if(payload.getEvent() instanceof ManualOverrideEvent event) {
            processOverride(event);
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

            // First check to see if anything is queued, if it is then remove it.
            if(scheduler.hasScheduledJobs(component.getComponentId())) {
                scheduler.cancelJobs(component.getComponentId());
            }

            DownlinkFrame frame = actuator.manualRequest(event, component.getSerialNumber(), component.getConfig());
            scheduler.create(component, frame, frame.getId());
        }

        System.out.println("Manual request received.");
    }

    private void processOverride(ManualOverrideEvent event) {
        System.out.println("Manual override recieved.");
    }

}
