package coop.shared.pi.config;

import coop.device.types.DeviceType;
import lombok.Data;

import java.util.Map;

@Data
public class ComponentState {
    private String componentId;
    private String serialNumber;
    private DeviceType deviceType;
    private Map<String, String> config;
    private Map<Integer, Map<String, String>> portConfig;
    private String groupId;

    public boolean isAlwaysOn() {
        return config != null && config.containsKey("always_on") && "true".equalsIgnoreCase(config.get("always_on"));
    }
}
