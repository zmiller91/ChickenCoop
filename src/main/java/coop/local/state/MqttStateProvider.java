package coop.local.state;

import coop.local.mqtt.*;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.IotShadowRequest;
import coop.shared.pi.config.IotState;
import coop.shared.pi.metric.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MqttStateProvider extends LocalStateProvider {

    @Autowired
    private PiMqttClient client;

    @Override
    public void init() {
        connect();
        refreshState();
    }

    @Override
    public void put(CoopState coopState) {
        super.put(coopState);
        reportBackToIot();
    }

    /**
     * Sends an empty message to the MQTT topic to retrieve the state from AWS IOT.
     */
    @Override
    public void refreshState() {
        PiMqttMessage message = new PiMqttMessage(ShadowTopic.GET.topic(), "{}");
        publishToMqtt(message);
    }

    /**
     * Sends a ShadowTopic.METRIC message to the remote server via MQTT.
     *
     * @param metric to send
     */
    @Override
    public void save(Metric metric) {
        PiMqttMessage mqttMessage = new PiMqttMessage(ShadowTopic.METRIC.topic(), metric);
        client().publish(mqttMessage);
    }

    /**
     * Publishes a message to the remote server via MQTT.
     *
     * @param message to send
     */
    private void publishToMqtt(PiMqttMessage message) {
        client().publish(message);
    }

    /**
     * Connect to MQTT and setup subscriptions. Wait a few seconds for the subscriptions and connection to take effect.
     */
    private void connect() {
        try {
            client().withSubscriptions(Arrays.asList(
                    new UpdateSubscription(this),
                    new SyncConfigSubscription(this)
            )).connect();

//            sleep(2000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method for getting the MQTT client.
     *
     * @return mqtt client
     */
    private PiMqttClient client() {
        if (this.client == null) {
            throw new IllegalStateException("Client is not initialized.");
        }

        return this.client;
    }

    /**
     * Send the state to AWS IOT;
     */
    private void reportBackToIot() {

        IotState state = new IotState();
        state.setReported(getConfig());

        IotShadowRequest response = new IotShadowRequest();
        response.setState(state);

        PiMqttMessage report = new PiMqttMessage(ShadowTopic.UPDATE.topic(), response);
        publishToMqtt(report);
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
