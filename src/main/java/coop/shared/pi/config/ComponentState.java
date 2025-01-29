package coop.shared.pi.config;

import coop.shared.database.table.ComponentType;
import lombok.Data;

import java.util.Map;

@Data
public class ComponentState {
    private String componentId;
    private String serialNumber;
    private ComponentType componentType;
    private Map<String, String> config;
}
