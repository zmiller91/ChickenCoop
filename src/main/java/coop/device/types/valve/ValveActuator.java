package coop.device.types.valve;

import coop.device.Action;
import coop.device.Actuator;
import coop.device.ConfigKey;
import coop.device.Device;
import coop.device.PortCommand;
import coop.device.protocol.DownlinkFrame;
import coop.device.protocol.event.ManualOverrideEvent;
import coop.device.protocol.event.ManualRequestEvent;
import coop.device.protocol.event.StatusEvent;
import coop.device.protocol.parser.CommandEventParser;
import coop.device.protocol.parser.EventParser;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ValveActuator implements Device, Actuator {

    /**
     * Number of ports this device supports. Purely a cloud-side/UI concept today (used to seed
     * ComponentPort display names and populate zone pickers) - the wire protocol doesn't validate
     * against it.
     */
    public static final int PORT_COUNT = 8;

    @Override
    public String getDescription() {
        return "Water Valve";
    }

    @Override
    public EventParser getEventParser() {
        return new CommandEventParser();
    }

    @Override
    public ConfigKey[] getConfig() {
        ConfigKey isAlwaysOn = new ConfigKey("always_on", "Always On");
        return new ConfigKey[]{isAlwaysOn};
    }

    @Override
    public ConfigKey[] getPortConfig() {
        ConfigKey defaultDuration = new ConfigKey("default_duration", "Default Duration");
        ConfigKey manualCutoff = new ConfigKey("manual_cutoff", "Manual Cutoff");
        return new ConfigKey[]{defaultDuration, manualCutoff};
    }

    @Override
    public boolean validateCommand(String name, Map<String, String> params) {
        ValveAction action = ValveAction.findByName(name);
        return action != null && action.getCommand().isValid(params);
    }

    @Override
    public DownlinkFrame createCommand(String serialNumber, String name, Map<String, String> params) {
        if(!validateCommand(name, params)) {
            return null;
        }
        ValveAction action = ValveAction.findByName(name);
        return action.getCommand().getCommand(serialNumber, params);
    }

    @Override
    public List<Action> getActions() {
        return Arrays.stream(ValveAction.values()).map(ValveAction::getAction).toList();
    }

    @Override
    public DownlinkFrame manualRequest(ManualRequestEvent event, String serialNumber, Map<String, String> componentConfig, Map<Integer, Map<String, String>> portConfig) {

        String[] payload = event.getPayload().split(DownlinkFrame.DELIMITER);
        if(payload.length != 2) {
            return null;
        }

        String requestedState = payload[1];
        String requestedZone = payload[0];

        if(!("ON".equals(requestedState) || "OFF".equals(requestedState))) {
            return null;
        }

        Map<String, String> zoneConfig = portConfig != null
                ? portConfig.get(Integer.parseInt(requestedZone))
                : null;

        if(zoneConfig == null || !zoneConfig.containsKey("default_duration")) {
            return null;
        }

        ValveAction action = "ON".equals(requestedState) ? ValveAction.TURN_ON : ValveAction.TURN_OFF;

        return createCommand(serialNumber,
                action.name(),
                Map.of("duration", zoneConfig.get("default_duration"),
                        "zone", requestedZone));
    }

    @Override
    public DownlinkFrame manualOverride(ManualOverrideEvent event, String serialNumber, Map<String, String> componentConfig) {

        if(componentConfig == null || !componentConfig.containsKey("manual_cutoff")) {
            return null;
        }

        if(!("ON".equals(event.getPayload()) || "OFF".equals(event.getPayload()))) {
            return null;
        }

        ValveAction action = "ON".equals(event.getPayload()) ? ValveAction.TURN_ON : ValveAction.TURN_OFF;

        return createCommand(serialNumber,
                action.name(),
                Map.of("duration", componentConfig.get("manual_cutoff")));
    }

    /**
     * Determines if the first frame can replace the second frame. That is, if they are the same request for the
     * same target.
     *
     * @param firstFrame
     * @param secondFrame
     * @return
     */
    @Override
    public boolean supersedes(DownlinkFrame firstFrame, DownlinkFrame secondFrame) {
        ValveAction[] actions = ValveAction.values();

        ValveAction firstAction = Stream.of(actions)
                .filter(a -> a.getCommand().isCommand(firstFrame))
                .findFirst()
                .orElse(null);

        ValveAction secondAction = Stream.of(actions)
                .filter(a -> a.getCommand().isCommand(secondFrame))
                .findFirst()
                .orElse(null);

        if(firstAction == null || secondAction == null) {
            return false;
        }

        if(firstAction != secondAction) {
            return false;
        }

        String firstTarget = firstAction.getCommand().getTarget(firstFrame);
        String secondTarget = secondAction.getCommand().getTarget(secondFrame);
        if(firstTarget == null || secondTarget == null) {
            return false;
        }

        return firstTarget.equals(secondTarget);
    }

    @Override
    public PortCommand describeFrame(DownlinkFrame frame) {
        ValveAction action = Stream.of(ValveAction.values())
                .filter(a -> a.getCommand().isCommand(frame))
                .findFirst()
                .orElse(null);

        if(action == null) {
            return null;
        }

        String port = action.getCommand().getPort(frame);
        if(port == null || !NumberUtils.isParsable(port)) {
            return null;
        }

        return new PortCommand(action.name(), Integer.parseInt(port));
    }

    /**
     * TURN_ON/TURN_OFF status types report a zone's actual on/off state directly (payload is just the zone
     * index) - unrelated to any specific command's lifecycle. Any other status type (e.g. battery) isn't
     * something this actuator owns.
     */
    @Override
    public PortCommand describePortStatus(StatusEvent event) {
        ValveAction action = ValveAction.findByName(event.getType());
        if(action == null) {
            return null;
        }

        if(!NumberUtils.isParsable(event.getPayload())) {
            return null;
        }

        return new PortCommand(event.getType(), Integer.parseInt(event.getPayload()));
    }
}
