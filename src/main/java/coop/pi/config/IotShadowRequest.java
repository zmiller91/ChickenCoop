package coop.pi.config;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class IotShadowRequest {
    private Long version;
    private Long timestamp;
    private IotState state;
    private JsonObject metadata;
}
