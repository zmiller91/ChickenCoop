package coop.local.mqtt;

import coop.shared.pi.events.HubEvent;

public class HubEventMessage extends PiMqttMessage {

    public HubEventMessage(HubEvent event) {
        super(ShadowTopic.EVENT.topic(), new Payload(event.getClass().getName(), event));
    }

    public record Payload(String clazz, Object object){};
}
