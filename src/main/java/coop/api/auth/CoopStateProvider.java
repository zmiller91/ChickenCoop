package coop.api.auth;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.google.gson.Gson;
import coop.database.repository.ComponentRepository;
import coop.database.table.ComponentConfig;
import coop.database.table.Coop;
import coop.pi.config.ComponentState;
import coop.pi.config.CoopState;
import coop.pi.config.IotShadowRequest;
import coop.pi.config.IotState;
import coop.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoopStateProvider {

    @Autowired
    private AWSIotData awsIot;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private AuthContext userContext;

    public CoopState forCoop(Coop coop) {

        List<ComponentState> components = componentRepository
                .findByCoop(coop)
                .stream()
                .map( component -> {

                    Map<String, String> config = new HashMap<>();
                    for (ComponentConfig c : component.getConfig()) {
                        config.put(c.getKey(), c.getValue());
                    }

                    ComponentState state = new ComponentState();
                    state.setComponentId(component.getComponentId());
                    state.setConfig(config);
                    return state;

                }).toList();


        CoopState config = new CoopState();
        config.setCoopId(coop.getId());
        config.setComponents(components);
        return config;

    }

    public void put(Coop coop, CoopState coopState) {

        IotState state = new IotState();
        state.setDesired(coopState);

        IotShadowRequest iotShadowRequest = new IotShadowRequest();
        iotShadowRequest.setState(state);

        UpdateThingShadowRequest updateRequest = new UpdateThingShadowRequest();
        updateRequest.setThingName(coop.getPi().getAwsIotThingId());
        updateRequest.setPayload(ByteBuffer.wrap(new Gson().toJson(iotShadowRequest).getBytes()));

        awsIot.updateThingShadow(updateRequest);
    }

    public CoopState getReported(Coop coop) {

        GetThingShadowRequest request = new GetThingShadowRequest();
        request.setThingName(coop.getPi().getAwsIotThingId());
        GetThingShadowResult result = awsIot.getThingShadow(request);
        String data = StandardCharsets.UTF_8.decode(result.getPayload()).toString();

        Gson gson = new Gson();
        IotShadowRequest body = gson.fromJson(data, IotShadowRequest.class);
        return body.getState().getReported();
    }

}
