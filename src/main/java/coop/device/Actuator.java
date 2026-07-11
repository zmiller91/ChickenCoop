package coop.device;

import com.google.gson.JsonObject;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.ManualOverrideEvent;
import coop.device.protocol.event.ManualRequestEvent;
import coop.device.protocol.event.StatusEvent;

import java.util.List;
import java.util.Map;

public interface Actuator {
    boolean validateCommand(String commandName, Map<String, String> params);
    DownlinkFrame createCommand(String serialNumber, String commandName, Map<String, String> params);
    List<Action> getActions();
    DownlinkFrame manualRequest(ManualRequestEvent event, String serialNumber, Map<String, String> componentConfig, Map<Integer, Map<String, String>> portConfig);
    DownlinkFrame manualOverride(ManualOverrideEvent event, String serialNumber, Map<String, String> componentConfig);
    boolean supersedes(DownlinkFrame firstFrame, DownlinkFrame secondFrame);

    /**
     * What a frame represents in port-oriented terms (which action, which port), or null if this
     * actuator doesn't recognize it. Used to report a job's outcome back up without the reporter needing to
     * know the device-specific frame format.
     */
    PortCommand describeFrame(DownlinkFrame frame);

    /**
     * Interprets a device-reported status broadcast as a port state change, or null if this event's type
     * isn't a port-state status this actuator owns. This is deliberately narrow - StatusEvent is a generic,
     * multi-purpose broadcast (its "type" could just as easily be something unrelated to ports at all, e.g.
     * a battery level), so PortCommand only fits the subset of status types that describe a port's on/off
     * state. Any other status type needs its own listener on StatusEvent rather than being forced through
     * this method or represented as a PortCommand.
     */
    PortCommand describePortStatus(StatusEvent event);
}
