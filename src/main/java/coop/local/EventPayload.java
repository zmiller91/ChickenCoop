package coop.local;

import coop.device.protocol.event.Event;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EventPayload {
    private Event event;
    private CoopState coop;
    private ComponentState component;
}