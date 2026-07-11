package coop.device.protocol.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RemoteManualCommandEvent implements Event {

    private String actionKey;
    private Map<String, String> params;

}
