package coop.local.listener;

import coop.device.protocol.UplinkFrame;
import coop.device.protocol.event.*;
import coop.device.protocol.parser.EventParser;
import coop.device.types.DeviceType;
import coop.local.EventPayload;
import coop.local.comms.message.MessageReceived;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;

import java.util.*;

/**
 * This is a static class that facilitates the parsing of messages and doling them out to listeners that care.
 * Because the main execution method (recieveMessage) requires a CoopState, it needs to be called from another
 * listener that hooks off the Communication's MessageReceived listener.
 */
public class EventProcessor {

    private static final Map<Class<? extends Event>, List<EventListener>> LISTENERS = new HashMap<>();

    public static void addListeners(EventListener... listener) {
        Arrays.stream(listener).forEach(EventProcessor::addListener);
    }

    public static void addListener(EventListener listener) {
        for(Class<? extends Event> clazz : listener.listenForClasses()) {
            LISTENERS.computeIfAbsent(clazz, k -> new ArrayList<>()).add(listener);
        }
    }

    public static void receiveMessage(CoopState coop, MessageReceived message) {
        if (coop == null) {
            return;
        }

        UplinkFrame frame = new UplinkFrame(message.getMessage());
        if(frame.getSerialNumber() == null) {
            return;
        }

        ComponentState component = coop
                .getComponents()
                .stream()
                .filter(c -> frame.getSerialNumber().equals(c.getSerialNumber()))
                .findFirst()
                .orElse(null);

        if(component == null) {
            return;
        }

        DeviceType deviceType = component.getDeviceType();
        EventParser parser = deviceType.getDevice().getEventParser();
        if(parser == null) {
            return;
        }

        List<Event> events = parser.parse(frame);
        if(events == null) {
            return;
        }

        for (Event event : events) {

            Class<? extends Event> eventClass = event.getClass();
            List<EventListener> listeners = LISTENERS.get(eventClass);
            if(listeners != null) {
                EventPayload payload = new EventPayload(event, coop, component);
                listeners.forEach((l) -> l.receive(payload));
            }
        }
    }
}
