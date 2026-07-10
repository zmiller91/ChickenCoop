package coop.local;

import coop.device.Actuator;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.Event;
import coop.device.protocol.event.ManualOverrideEvent;
import coop.device.protocol.event.ManualRequestEvent;
import coop.local.database.job.Job;
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

            // Create the downlink frame from the request
            DownlinkFrame requestedFrame = actuator.manualRequest(event, component.getSerialNumber(), component.getConfig());
            if(requestedFrame != null) {

                // Find jobs that are queued and cancel any where the new job will supersede the existing job.
                List<Job> jobs = scheduler.getUnSubmittedJobs(component.getComponentId());
                for (Job job : jobs) {
                    DownlinkFrame jobFrame = DownlinkFrame.fromString(job.getDownlink().getFrame());
                    if (actuator.supersedes(jobFrame, requestedFrame)) {
                        scheduler.cancelJob(job);
                    }
                }

                scheduler.create(component, requestedFrame, requestedFrame.getId());
            }
        }

        System.out.println("Manual request received.");
    }

    private void processOverride(ManualOverrideEvent event) {
        System.out.println("Manual override recieved.");
    }

}
