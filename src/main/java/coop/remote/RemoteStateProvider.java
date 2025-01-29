package coop.remote;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.google.gson.Gson;
import coop.shared.database.table.Coop;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.pi.config.IotShadowRequest;
import coop.shared.pi.config.IotState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

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

}
