package coop.local.listener;

import coop.device.protocol.event.Event;
import coop.local.EventPayload;

import java.util.List;
import java.util.function.Consumer;

public interface EventListener {
    List<Class<? extends Event>> listenForClasses();
    void receive(EventPayload payload);
}
