package coop.remote.schedule;

import coop.shared.pi.events.HubEventPayload;
import lombok.Data;

@Data
public class IoTMessage {
    private String principal;
    private String clientId;
    private HubEventPayload event;
}
