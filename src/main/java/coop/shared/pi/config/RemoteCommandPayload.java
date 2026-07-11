package coop.shared.pi.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Wire shape for a one-shot command published by coop.remote and consumed by coop.local's CommandSubscription.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoteCommandPayload {

    private String componentId;
    private String actionKey;
    private Map<String, String> params;

}
