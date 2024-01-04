package coop.local.mqtt;

import com.google.gson.Gson;
import coop.local.LocalStateProvider;
import coop.shared.pi.config.IotShadowRequest;
import coop.shared.pi.config.IotState;
import software.amazon.awssdk.crt.mqtt.MqttMessage;

public class UpdateSubscription extends ShadowSubscription {

    private final LocalStateProvider provider;

    public UpdateSubscription(LocalStateProvider provider) {
        super(ShadowTopic.UPDATE_ACCEPTED);
        this.provider = provider;
    }

    @Override
    public void accept(MqttMessage raw) {
        super.accept(raw);

        Gson gson = new Gson();
        IotShadowRequest request = gson.fromJson(new String(raw.getPayload()), IotShadowRequest.class);
        IotState state = request.getState();

        if (state.getDesired() != null) {
            provider.setConfig(state.getDesired());
            provider.reportBackToIot();
        }
    }
}
