package coop.shared.pi.events;

import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HubEventPayload {
    private HubEventType type;
    private JsonElement payload;
}
