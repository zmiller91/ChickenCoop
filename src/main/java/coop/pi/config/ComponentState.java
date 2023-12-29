package coop.pi.config;

import lombok.Data;

import java.util.Map;

@Data
public class ComponentState {
    private String componentId;
    private Map<String, String> config;
}
