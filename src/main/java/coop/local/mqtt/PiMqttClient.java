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

    private final String clientEndpoint;
    private final String clientId;
    private final String certPath;
    private final String privateKeyPath;


    private MqttClientConnection connection;
    private boolean isConnected = false;

    private long lastConnectionAttempt = 0;
    private List<ShadowSubscription> subscriptions = new ArrayList<>();

    public PiMqttClient(String clientEndpoint, String clientId, String certPath, String privateKeyPath) {

        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.certPath = certPath;
        this.privateKeyPath = privateKeyPath;

        createConnection();
    }

    private void createConnection() {
        try(AwsIotMqttConnectionBuilder builder = AwsIotMqttConnectionBuilder.newMtlsBuilderFromPath(certPath, privateKeyPath)
                .withConnectionEventCallbacks(this)
                .withClientId(clientId)
                .withEndpoint(clientEndpoint)
                .withCleanSession(false)) {
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

    private boolean subscribe() {
        try {
            for (ShadowSubscription subscription : subscriptions) {
                connection.subscribe(subscription.getTopicName(), QualityOfService.AT_LEAST_ONCE, subscription).get();
            }

            return true;
        } catch (Exception e) {
            System.out.println("Failed to subscribe. " + e.getMessage());
            return false;
        }
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

            if(connection == null) {
                return false;
            }

            CompletableFuture<Boolean> connected = connection.connect();
            boolean isResumedSession = connected.get();
            if (!isResumedSession) {
                return subscribe();
            }

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
    public void onConnectionResumed(boolean sessionPresent) {
        isConnected = true;
        System.out.println("Connection was resumed");

        if (!sessionPresent) {
            System.out.println("Session not present. Resubscribing to topics...");
            subscribe();
        }
    }

    @Override
    public void onConnectionSuccess(OnConnectionSuccessReturn data) {
        isConnected = true;
        System.out.println("Connection was successful");
    }

    @Override
    public void onConnectionFailure(OnConnectionFailureReturn data) {
        isConnected = false;
        System.out.println("Connection failed with code " + data.getErrorCode() + " . Recreating connection.");
        connection.disconnect().join();
        connection.close();
        createConnection();
        System.out.println("Creating connection.");
    }

    @Override
    public void onConnectionClosed(OnConnectionClosedReturn data) {
        isConnected = false;
        System.out.println("Connection was closed");
    }
}
