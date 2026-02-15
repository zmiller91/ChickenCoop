package coop.shared.pi.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HubEventPayload {
    private HubEventType type;
    private Object payload;
}
