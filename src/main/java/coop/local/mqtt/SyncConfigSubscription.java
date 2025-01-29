package coop.local.mqtt;

import com.google.gson.Gson;
import coop.local.state.LocalStateProvider;
import coop.shared.pi.config.IotShadowRequest;
import coop.shared.pi.config.IotState;
import software.amazon.awssdk.crt.mqtt.MqttMessage;

public class SyncConfigSubscription extends ShadowSubscription {

    private final LocalStateProvider provider;

    public SyncConfigSubscription(LocalStateProvider provider) {
        super(ShadowTopic.GET_ACCEPTED);
        this.provider = provider;
    }

    @Override
    public void accept(MqttMessage raw) {
        super.accept(raw);

        try {
            System.out.println("Syncing config...");

            Gson gson = new Gson();
            IotShadowRequest request = gson.fromJson(new String(raw.getPayload()), IotShadowRequest.class);
            IotState state = request.getState();
            provider.put(state.getDesired());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
