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
    private String groupId;
}
