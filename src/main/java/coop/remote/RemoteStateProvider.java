package coop.remote;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.model.PublishRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.google.gson.Gson;
import coop.shared.database.table.Coop;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.IotShadowRequest;
import coop.shared.pi.config.IotState;
import coop.shared.pi.config.RemoteCommandPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Map;

@Component
public class RemoteStateProvider extends StateProvider {

    @Autowired
    private AWSIotData awsIot;

    public void put(CoopState coopState) {

        IotState state = new IotState();
        state.setDesired(coopState);

        IotShadowRequest iotShadowRequest = new IotShadowRequest();
        iotShadowRequest.setState(state);

        UpdateThingShadowRequest updateRequest = new UpdateThingShadowRequest();
        updateRequest.setThingName(coopState.getAwsIotThingId());
        updateRequest.setPayload(ByteBuffer.wrap(new Gson().toJson(iotShadowRequest).getBytes()));

        awsIot.updateThingShadow(updateRequest);
    }

    /**
     * Publishes a one-shot command directly to the Pi's command topic, bypassing the durable shadow document -
     * the Pi's CommandSubscription picks this up and dispatches it immediately, it's never stored as desired
     * state.
     */
    @Override
    public void sendCommand(Coop coop, String componentId, String actionKey, Map<String, String> params) {

        RemoteCommandPayload payload = new RemoteCommandPayload(componentId, actionKey, params);

        // Must match ShadowTopic.COMMAND's "hub_command/%s" template on the Pi side. Can't share that
        // constant directly - ShadowTopic.topic() resolves %s via a PiContext bean that only exists in
        // coop.local's Spring context (one Pi's own identity, read from a local file); the cloud talks to
        // many Pis and has to build the topic per-request from whichever Coop/Pi it's handling.
        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setTopic("hub_command/" + coop.getPi().getAwsIotThingId());
        publishRequest.setQos(1);
        publishRequest.setPayload(ByteBuffer.wrap(new Gson().toJson(payload).getBytes()));

        awsIot.publish(publishRequest);
    }

}
