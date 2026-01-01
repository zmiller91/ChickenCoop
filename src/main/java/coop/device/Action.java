package coop.device;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Action {

    private String key;
    private String displayName;
    private String description;
    private String[] params;

}
