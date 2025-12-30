package coop.local;

import coop.device.protocol.UplinkFrame;
import coop.device.protocol.event.*;
import coop.device.protocol.parser.EventParser;
import coop.device.types.DeviceType;
import coop.local.comms.message.MessageReceived;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;

import java.util.*;
import java.util.function.Consumer;

/**
 * This is a static class that facilitates the parsing of messages and doling them out to listeners that care.
 * Because the main execution method (recieveMessage) requires a CoopState, it needs to be called from another
 * listener that hooks off the Communication's MessageReceived listener.
 */
public class EventListener {

    private static final List<Class<? extends Event>> EVENT_LIST = Arrays.asList(
            MetricEvent.class,
            AckEvent.class,
            CommandRequestEvent.class,
            CommandCompleteEvent.class
    );

    private static final Map<Class<? extends Event>, List<Consumer<EventPayload>>> LISTENERS = new HashMap<>();

    public static <E extends Event> void addListener(Class<? extends Event> eventType, Consumer<EventPayload> listener) {
        LISTENERS.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public static void receiveMessage(CoopState coop, MessageReceived message) {
        if(coop != null) {

            UplinkFrame frame = new UplinkFrame(message.getMessage());
            if(frame.getSerialNumber() != null) {

                ComponentState component = coop
                        .getComponents()
                        .stream()
                        .filter(c -> c.getSerialNumber().equals(frame.getSerialNumber()))
                        .findFirst()
                        .orElse(null);

                if(component != null) {

                    DeviceType deviceType = component.getDeviceType();
                    EventParser parser = deviceType.getDevice().getEventParser();
                    if(parser != null) {

                        List<Event> events = parser.parse(frame);
                        if (events != null) {

                            for (Event event : events) {

                                for(Class<? extends Event> clazz : EVENT_LIST) {
                                    if(clazz.isInstance(event)) {

                                        List<Consumer<EventPayload>> listeners = LISTENERS.get(clazz);
                                        if(listeners != null) {
                                            EventPayload payload = new EventPayload(event, coop, component);
                                            listeners.forEach((l) -> l.accept(payload));
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
