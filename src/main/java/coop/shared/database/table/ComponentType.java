package coop.shared.database.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum ComponentType {

    DOOR("DOOR",
            new ConfigKey("door.open", "Door Open Time"),
            new ConfigKey("door.close", "Door Close Time")),
    WEATHER("WEATHER"),
    FOOD("FOOD", new ConfigKey("food.alert.threshold", "Alert Threshold")),
    WATER("WATER", new ConfigKey("water.alert.threshold", "Alert Threshold"));

    private String name;
    private ConfigKey[] config;

    ComponentType(String name) {
        this(name, new ConfigKey[]{});
    }

    ComponentType(String name, ConfigKey... config) {
        this.name = name;
        this.config = config;
    }

    public List<ComponentConfig> initialConfig(CoopComponent component) {
        return Stream.of(this.config).map(c -> {
            ComponentConfig cc = new ComponentConfig();
            cc.setComponent(component);
            cc.setKey(c.key);
            cc.setValue("");
            return cc;
        }).toList();
    }

    public Map<String, String> keyDisplayNames() {
        Map<String, String> map = new HashMap<>();
        for(ConfigKey key : config) {
            map.put(key.key, key.displayName);
        }

        return map;
    }

    public static ComponentType getByName(String name) {
        return Stream.of(ComponentType.values())
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    public record ConfigKey(String key, String displayName){}

}
