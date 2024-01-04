package coop.local;

import com.google.gson.Gson;
import coop.local.mqtt.*;
import coop.local.service.PiRunner;
import coop.shared.pi.metric.Metric;
import lombok.Data;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class CoopRunner extends PiRunner {

    private static final Duration PUBLISH_DURATION = Duration.ofSeconds(30);
    private static final long MQTT_TIMEOUT = 5000;

    @Autowired
    private LocalStateProvider provider;

    private long lastPublish = 0;
    private double foodLbs = 25;
    private double waterPct = 100;

    @Override
    protected void init() {
        PiMqttMessage message = new PiMqttMessage(ShadowTopic.GET.topic(), "{}");
        publish(message);

    }

    private void getWeather(Consumer<Weather> callback) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get("https://api.open-meteo.com/v1/forecast?latitude=41.661129&longitude=-91.530167&current=temperature_2m,relative_humidity_2m&temperature_unit=fahrenheit&timeformat=unixtime").build();

//            httpclient.execute(httpGet, response -> {
//                String content = CharStreams.toString(new InputStreamReader(response.getEntity().getContent()));
//                System.out.println(content);
//                Weather weather = GSON.fromJson(content, Weather.class);
//                EntityUtils.consume(response.getEntity());
//                callback.accept(weather);
//                return weather;
//            });
        } catch (IOException e){
            throw new UncheckedIOException(e);
        }
    }

    private double getFoodLbs() {
        double food = foodLbs;
        foodLbs = foodLbs - 1;
        if (foodLbs <= 0) {
            foodLbs = 25;
        }

        return Math.max(0, food);
    }

    private double getWaterPct() {
        double water = waterPct;
        waterPct = waterPct - 10;
        if (waterPct <= 0) {
            waterPct = 100;
        }

        return Math.max(water, 0);
    }

    @Override
    protected void invoke() {

        long timeSinceLastPublish = System.currentTimeMillis() - lastPublish;
        if (timeSinceLastPublish >= PUBLISH_DURATION.toMillis()) {

            System.out.println("State: " + new Gson().toJson(provider.getConfig()));

            getWeather((weather) -> {
                publish("component-1234", "temperature", weather.getCurrent().getTemperature_2m());
                publish("component-1234", "humidity", weather.getCurrent().getRelative_humidity_2m());
            });

//            publish("component-1234", "door_open", isDoorOpen() ? 1 : 0);
            publish("component-1234", "food", getFoodLbs());
            publish("component-1234", "water", getWaterPct());
            lastPublish = System.currentTimeMillis();
        }
    }

    @Override
    protected void handleError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @Override
    public List<ShadowSubscription> subscriptions() {
        return Arrays.asList(
                new UpdateSubscription(provider),
                new SyncConfigSubscription(provider)
        );
    }

    private void publish(String component, String metricName, double value) {
        if (this.provider.getConfig() == null || this.provider.getConfig().getCoopId() == null) {
            return;
        }

        Metric metric = new Metric();
        metric.setDt(System.currentTimeMillis());
        metric.setCoopId(this.provider.getConfig().getCoopId());
        metric.setComponentId(component);
        metric.setMetric(metricName);
        metric.setValue(value);

        PiMqttMessage message = new PiMqttMessage(ShadowTopic.METRIC.topic(), metric);
        publish(message);
    }

    private void publish(PiMqttMessage message) {
        // TODO: have to save the data locally...
        client().publish(message);
    }

    @Data
    private static class Weather {
        private Current current;
    }

    @Data
    private static class Current {
        private long time;
        private double temperature_2m;
        private double relative_humidity_2m;
    }
}
