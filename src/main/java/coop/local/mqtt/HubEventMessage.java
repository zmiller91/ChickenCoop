package coop.local.mqtt;

import coop.shared.pi.events.HubEvent;
import coop.shared.pi.events.HubEventPayload;

public class HubEventMessage extends PiMqttMessage {

    public HubEventMessage(HubEvent event) {
        super(ShadowTopic.EVENT.topic(), new HubEventPayload(event.getType(), event));
    }

}
