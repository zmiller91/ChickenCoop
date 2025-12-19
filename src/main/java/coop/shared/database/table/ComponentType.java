package coop.shared.database.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum ComponentType {

    DOOR("DOOR",
            "Door",
            new ConfigKey("door.open", "Door Open Time"),
            new ConfigKey("door.close", "Door Close Time")),
    WEATHER("WEATHER", "Weather Sensor"),
    FOOD("FOOD", "Food Level Monitor", new ConfigKey("food.alert.threshold", "Alert Threshold")),
    WATER("WATER", "Water Level Monitor", new ConfigKey("water.alert.threshold", "Alert Threshold")),
    SCALE("Scale", "Weight Monitor"),
    MOISTURE("Moisture", "Moisture Level Monitor");

    private String name;
    private String descr;
    private ConfigKey[] config;

    ComponentType(String name, String descr) {
        this(name, descr, new ConfigKey[]{});
    }

    ComponentType(String name, String descr, ConfigKey... config) {
        this.name = name;
        this.descr = descr;
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

    public String getDescription() {
        return descr;
    }

    public static ComponentType getByName(String name) {
        return Stream.of(ComponentType.values())
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    public record ConfigKey(String key, String displayName){}

}
