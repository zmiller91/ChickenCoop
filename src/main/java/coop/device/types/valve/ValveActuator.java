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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
     * STATE reports every port's current on/off state in one combined broadcast (sent both on-change and
     * periodically for self-healing) - a fixed-width bitmap where character i is '1' (ON) or '0' (OFF) for
     * port i. Unlike a per-port status, this always describes every port unconditionally regardless of
     * whether it actually changed; PortStatusProcessor is responsible for diffing against last-known state
     * before deciding what's worth logging. Any other status type (e.g. battery) isn't something this
     * actuator owns.
     */
    @Override
    public List<PortCommand> describePortStatus(StatusEvent event) {
        if(!"STATE".equals(event.getType())) {
            return Collections.emptyList();
        }

        String payload = event.getPayload();
        if(payload == null || payload.length() != PORT_COUNT) {
            return Collections.emptyList();
        }

        List<PortCommand> commands = new ArrayList<>(PORT_COUNT);
        for(int i = 0; i < PORT_COUNT; i++) {
            char bit = payload.charAt(i);
            if(bit != '0' && bit != '1') {
                return Collections.emptyList();
            }

            ValveAction action = bit == '1' ? ValveAction.TURN_ON : ValveAction.TURN_OFF;
            commands.add(new PortCommand(action.name(), i));
        }

        return commands;
    }
}
