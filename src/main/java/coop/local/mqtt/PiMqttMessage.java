package coop.local.mqtt;

import com.google.gson.Gson;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.nio.charset.StandardCharsets;

public class PiMqttMessage {

    private static final Gson GSON = new Gson();

    private final String topic;
    private final String payload;

    public PiMqttMessage(String topic, Object payload) {
        this(topic, GSON.toJson(payload));
    }

    public PiMqttMessage(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
    }

    public MqttMessage message() {
        return new MqttMessage(topic, payload.getBytes(StandardCharsets.UTF_8), QualityOfService.AT_LEAST_ONCE);
    }

    public void onSuccess() {
        System.out.println("Successfully published message: " + payload);
    }

    public Void onFailed(Throwable t) {
        System.out.println("Failed to publish message due to: " + t.getMessage());
        return null;
    }


}
