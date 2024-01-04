package coop.local.mqtt;


import software.amazon.awssdk.crt.mqtt.MqttMessage;

import java.util.function.Consumer;

public class ShadowSubscription implements Consumer<MqttMessage> {

    private final ShadowTopic topic;

    public ShadowSubscription(ShadowTopic topic) {
        this.topic = topic;
    }

    public String getTopicName() {
        return topic.topic();
    }

    @Override
    public void accept(MqttMessage mqttMessage) {
        System.out.println(
                "\n---------- BEGIN " + mqttMessage.getTopic() + "-----------\n" +
                new String(mqttMessage.getPayload()) +
                "\n----------- END " + mqttMessage.getTopic() + "------------\n"
        );
    }
}