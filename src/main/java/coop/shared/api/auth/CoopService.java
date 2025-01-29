package coop.shared.api.auth;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.MetricRepository;
import coop.shared.database.repository.PiRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.Pi;
import coop.shared.exception.BadRequest;
import coop.shared.exception.NotFound;
import coop.shared.pi.StateFactory;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private StateProvider stateProvider;

    @Autowired
    MetricRepository metricRepository;

    @Autowired
    private StateFactory stateFactory;

    @PostMapping("/register")
    public RegisterCoopResponse create(@RequestBody RegisterCoopRequest request) {

        Pi pi = piRepository.findById(request.id);
        if (pi == null) {
            throw new BadRequest("Serial number not found.");
        }

        Coop coop = coopRepository.create(userContext.getCurrentUser(), request.name, pi);

        CoopState state = stateFactory.forCoop(coop);
        stateProvider.put(state);

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

    @GetMapping("/{coopId}")
    public GetCoopResponse get(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        CoopDAO coopDAO = new CoopDAO(coop.getId(), coop.getName());
        return new GetCoopResponse(coopDAO);
    }

    @GetMapping("/settings/{coopId}")
    public GetCoopSettingsResponse getSettings(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        String message = "Temporary method, this broke.";
        CoopSettingsDAO dao = new CoopSettingsDAO(message);
        return  new GetCoopSettingsResponse(dao);
    }

    @GetMapping("/data/{coopId}/{metric}")
    public GetCoopDataResponse getData(@PathVariable("coopId") String coopId, @PathVariable("metric") String metric) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<MetricRepository.MetricDataRow> data = metricRepository.findByMetricHourly(coop, metric);
        return new GetCoopDataResponse(coopId, data);
    }

    @PostMapping("/settings/{coopId}")
    public UpdateCoopSettingsResponse updateCoopSettings(@PathVariable("coopId") String coopId, @RequestBody UpdateCoopSettingsRequest request) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        CoopState coopState = stateFactory.forCoop(coop);
        stateProvider.put(coopState);

        return new UpdateCoopSettingsResponse(request.settings);
    }

    public record RegisterCoopRequest(String id, String name) {}
    public record RegisterCoopResponse(CoopDAO coop){}

    public record ListCoopResponse(List<CoopDAO> coops){}
    public record GetCoopResponse(CoopDAO coop){}

    public record CoopDAO(String id, String name){};

    public record CoopSettingsDAO(String message){};
    public record GetCoopSettingsResponse(CoopSettingsDAO settings){};

    public record UpdateCoopSettingsRequest(CoopSettingsDAO settings){};
    public record UpdateCoopSettingsResponse(CoopSettingsDAO settings){};

    public record GetCoopDataRequest(String coopId, String metric){}
    public record GetCoopDataResponse(String coopId, List<MetricRepository.MetricDataRow> data){}

}
