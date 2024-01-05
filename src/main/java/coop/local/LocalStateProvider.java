package coop.local;

import com.google.gson.Gson;
import coop.local.mqtt.PiMqttClient;
import coop.local.mqtt.PiMqttMessage;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.PiRepository;
import coop.shared.database.table.Pi;
import coop.local.mqtt.ShadowTopic;
import coop.shared.database.table.Coop;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.IotShadowRequest;
import coop.shared.pi.config.IotState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.nio.charset.StandardCharsets;

@Component
public class LocalStateProvider extends StateProvider {

    private static final long PUBLISH_TIMEOUT = 3000;
    private static final Gson GSON = new Gson();

    @Autowired
    private PiMqttClient client;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private Context context;

    private CoopState config = null;

    public CoopState getConfig() {
        if(config == null) {

            Pi pi = piRepository.findById(context.piId());
            Coop coop = coopRepository.findById(pi, "4028b2698ca7ff21018ca8004f970000");
            this.config = forCoop(coop);
            if (this.config == null) {
                throw new IllegalStateException("Config for coop not found.");
            }
        }

        return this.config;
    }

    public void setConfig(CoopState config) {
        this.config = config;
    }

    @Override
    public void put(Coop coop, CoopState coopState) {
        this.config = coopState;
    }

    public void reportBackToIot() {

        IotState state = new IotState();
        state.setReported(config);

        IotShadowRequest response = new IotShadowRequest();
        response.setState(state);

        PiMqttMessage report = new PiMqttMessage(ShadowTopic.UPDATE.topic(), response);
        this.client.publish(report);
    }
}
