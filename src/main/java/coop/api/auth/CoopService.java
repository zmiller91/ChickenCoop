package coop.api.auth;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.google.gson.Gson;
import coop.database.repository.CoopRepository;
import coop.database.repository.MetricRepository;
import coop.database.repository.PiRepository;
import coop.database.table.Coop;
import coop.database.table.CoopMetric;
import coop.database.table.Pi;
import coop.exception.BadRequest;
import coop.exception.NotFound;
import coop.pi.config.CoopConfig;
import coop.pi.config.IotShadowRequest;
import coop.pi.config.IotState;
import coop.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/coops")
public class CoopService {

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private AuthContext userContext;

    @Autowired
    private AWSIotData awsIot;

    @Autowired
    MetricRepository metricRepository;

    @PostMapping("/register")
    public RegisterCoopResponse create(@RequestBody RegisterCoopRequest request) {

        Pi pi = piRepository.findById(request.id);
        if (pi == null) {
            throw new BadRequest("Serial number not found.");
        }

        Coop coop = coopRepository.create(userContext.getCurrentUser(), request.name, pi);

        CoopConfig config = new CoopConfig();
        config.setWelcome("Hello " + userContext.getCurrentUser().getUsername());
        config.setCoopId(coop.getId());
        putCoopConfig(coop, config);

        return new RegisterCoopResponse(new CoopDAO(coop.getId(), coop.getName()));
    }

    @GetMapping("/list")
    public ListCoopResponse list() {
         List<CoopDAO> coops = coopRepository.list(userContext.getCurrentUser())
                 .stream()
                 .map(c -> new CoopDAO(c.getId(), c.getName()))
                 .toList();

         return new ListCoopResponse(coops);
    }

    @GetMapping("/settings/{coopId}")
    public GetCoopSettingsResponse getSettings(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        GetThingShadowRequest request = new GetThingShadowRequest();
        request.setThingName(coop.getPi().getAwsIotThingId());
        GetThingShadowResult result = awsIot.getThingShadow(request);
        String data = StandardCharsets.UTF_8.decode(result.getPayload()).toString();

        Gson gson = new Gson();
        IotShadowRequest body = gson.fromJson(data, IotShadowRequest.class);
        String message = body.getState().getDesired().getWelcome();

        CoopSettingsDAO dao = new CoopSettingsDAO(message);
        GetCoopSettingsResponse response = new GetCoopSettingsResponse(dao);
        return response;
    }

    @GetMapping("/data/{coopId}/{metric}")
    public GetCoopDataResponse getData(@PathVariable("coopId") String coopId, @PathVariable("metric") String metric) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<CoopMetricDAO> data = metricRepository.findByMetric(coop, metric).stream()
                .map(t -> {
                    Instant metricInstant = Instant.ofEpochMilli(t.getDt());
                    ZonedDateTime metricDate = ZonedDateTime.ofInstant(metricInstant, ZoneOffset.UTC);
                    String date = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").withZone(ZoneOffset.ofHours(-6))
                            .format(metricDate);

                    return new CoopMetricDAO(String.valueOf(t.getValue()), date);
                }).toList();


        return new GetCoopDataResponse(coopId, data);
    }

    @PostMapping("/settings/{coopId}")
    public UpdateCoopSettingsResponse updateCoopSettings(@PathVariable("coopId") String coopId, @RequestBody UpdateCoopSettingsRequest request) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        CoopConfig coopConfig = new CoopConfig();
        coopConfig.setWelcome(request.settings.message);

        putCoopConfig(coop, coopConfig);
        return new UpdateCoopSettingsResponse(request.settings);
    }

    private void putCoopConfig(Coop coop, CoopConfig coopConfig) {

        IotState state = new IotState();
        state.setDesired(coopConfig);

        IotShadowRequest iotShadowRequest = new IotShadowRequest();
        iotShadowRequest.setState(state);

        UpdateThingShadowRequest updateRequest = new UpdateThingShadowRequest();
        updateRequest.setThingName(coop.getPi().getAwsIotThingId());
        updateRequest.setPayload(ByteBuffer.wrap(new Gson().toJson(iotShadowRequest).getBytes()));

        awsIot.updateThingShadow(updateRequest);
    }

    public record RegisterCoopRequest(String id, String name) {}
    public record RegisterCoopResponse(CoopDAO coop){}

    public record ListCoopResponse(List<CoopDAO> coops){}

    public record CoopDAO(String id, String name){};

    public record CoopSettingsDAO(String message){};
    public record GetCoopSettingsResponse(CoopSettingsDAO settings){};

    public record UpdateCoopSettingsRequest(CoopSettingsDAO settings){};
    public record UpdateCoopSettingsResponse(CoopSettingsDAO settings){};

    public record GetCoopDataRequest(String coopId, String metric){}
    public record GetCoopDataResponse(String coopId, List<CoopMetricDAO> data){}
    public record CoopMetricDAO(String value, String date){}

}
