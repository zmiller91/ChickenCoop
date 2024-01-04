package coop.local.mqtt;

import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.crt.mqtt.*;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class PiMqttClient implements MqttClientConnectionEvents {

    private static final Duration CONNECTION_RETRY_INTERVAL = Duration.ofMinutes(3);

    private MqttClientConnection connection;
    private boolean isConnected = false;

    private long lastConnectionAttempt = 0;
    private List<ShadowSubscription> subscriptions = new ArrayList<>();

    public PiMqttClient(String clientEndpoint, String clientId, String certPath, String privateKeyPath) {
        try(AwsIotMqttConnectionBuilder builder = AwsIotMqttConnectionBuilder.newMtlsBuilderFromPath(certPath, privateKeyPath)
                .withConnectionEventCallbacks(this)
                .withClientId(clientId)
                .withEndpoint(clientEndpoint)) {
            connection = builder.build();
        }
    }

    public PiMqttClient withSubscriptions(List<ShadowSubscription> topics) {
        this.subscriptions = topics;
        return this;
    }

    public void publish(PiMqttMessage message) {

        if (!isConnected && !connect()) {
            return;
        }

        CompletableFuture.supplyAsync(() -> connection.publish(message.message()))
                .thenAccept(i -> message.onSuccess())
                .exceptionally(message::onFailed);
    }

    public boolean connect() {

        if(isConnected) {
            return true;
        }

        long timeSinceLastConnect = System.currentTimeMillis() - lastConnectionAttempt;
        if (lastConnectionAttempt != 0 && timeSinceLastConnect < CONNECTION_RETRY_INTERVAL.toMillis()) {
            log.info("Not attempting connection since insufficient time has passed.");
            return false;
        }

        try {
            CompletableFuture<Boolean> connected = connection.connect();
            boolean isResumedSession = connected.get();
            if (!isResumedSession) {
                for(ShadowSubscription subscription : subscriptions) {
                    connection.subscribe(subscription.getTopicName(), QualityOfService.AT_LEAST_ONCE, subscription).get();
                }
            }

            // TODO: As soon as a connection is established the Pi should ask for a state update.

            Thread.sleep(1000);
            return true;

        } catch (Exception e) {
            log.info("Error connecting to MQTT. Will retry later.");
            connection.disconnect();
            return false;

        } finally {
            lastConnectionAttempt = System.currentTimeMillis();
        }
    }

    @Override
    public void onConnectionInterrupted(int i) {
        isConnected = false;
        System.out.println("Connection was interrupted");
    }

    @Override
    public void onConnectionResumed(boolean b) {
        isConnected = true;
        System.out.println("Connection was resumed");
    }

    @Override
    public void onConnectionSuccess(OnConnectionSuccessReturn data) {
        isConnected = true;
        System.out.println("Connection was successful");
    }

    @Override
    public void onConnectionFailure(OnConnectionFailureReturn data) {
        isConnected = false;
        System.out.println("Connection failed");
    }

    @Override
    public void onConnectionClosed(OnConnectionClosedReturn data) {
        isConnected = false;
        System.out.println("Connection was closed");
    }
}
