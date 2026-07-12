package coop.local.mqtt;

import com.google.gson.Gson;
import coop.device.protocol.event.RemoteManualCommandEvent;
import coop.local.listener.EventProcessor;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.ComponentState;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.RemoteCommandPayload;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.crt.mqtt.MqttMessage;

@Log4j2
public class CommandSubscription extends ShadowSubscription {

    private final LocalStateProvider provider;

    public CommandSubscription(LocalStateProvider provider) {
        super(ShadowTopic.COMMAND);
        this.provider = provider;
    }

    @Override
    public void accept(MqttMessage raw) {
        super.accept(raw);

        try {
            Gson gson = new Gson();
            RemoteCommandPayload payload = gson.fromJson(new String(raw.getPayload()), RemoteCommandPayload.class);

            CoopState coop = provider.getConfig();
            if (coop == null) {
                log.warn("Dropping remote command - no config loaded yet.");
                return;
            }

            if (payload == null || payload.getComponentId() == null) {
                log.warn("Dropping remote command - payload missing or has no componentId: " + new String(raw.getPayload()));
                return;
            }

            ComponentState component = coop.getComponents()
                    .stream()
                    .filter(c -> payload.getComponentId().equals(c.getComponentId()))
                    .findFirst()
                    .orElse(null);

            if (component == null) {
                log.warn("Dropping remote command - no component " + payload.getComponentId() + " in the currently loaded config.");
                return;
            }

            EventProcessor.receiveRemoteCommand(coop, component, new RemoteManualCommandEvent(payload.getActionKey(), payload.getParams()));

        } catch (Exception e) {
            log.error("Failed to process remote command.", e);
        }
    }

}
