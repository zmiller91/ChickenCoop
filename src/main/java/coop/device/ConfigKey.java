package coop.device;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigKey {
    private final String key;
    private final String displayName;
}
