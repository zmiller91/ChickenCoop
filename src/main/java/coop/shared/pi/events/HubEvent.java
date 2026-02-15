package coop.shared.pi.events;

import lombok.Data;

@Data
public abstract class HubEvent {
    private String clientId;
    private long dt;
    private String coopId;

    public abstract HubEventType getType();
}
