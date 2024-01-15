package coop.shared.api.auth;

import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.MetricInterval;
import coop.shared.database.repository.MetricRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.CoopComponent;
import coop.shared.exception.NotFound;
import coop.shared.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/data")
public class MetricService {

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private AuthContext userContext;

    @Autowired
    private MetricRepository metricRepository;

    @GetMapping("/{coopId}")
    public GetCoopMetricsResult getCoopMetrics(@PathVariable("coopId") String coopId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<MetricRepository.ComponentData> data = metricRepository.findByCoop(coop, MetricInterval.DAY);
        return new GetCoopMetricsResult(data);
    }

    @GetMapping("/{coopId}/{componentId}/{interval}")
    public GetComponentMetricResult getComponentMetric(
            @PathVariable("coopId") String coopId,
            @PathVariable("componentId") String componentId,
            @PathVariable("interval") String intervalName) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        CoopComponent component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if(component == null || !component.getCoop().getId().equals(coop.getId())) {
            throw new NotFound("Component not found.");
        }

        MetricInterval interval = MetricInterval.valueOf(intervalName);
        MetricRepository.ComponentData data = metricRepository.findByCoopComponent(coop, component, interval);
        return new GetComponentMetricResult(data);
    }

    public record GetCoopMetricsResult(List<MetricRepository.ComponentData> data){};
    public record GetComponentMetricResult(MetricRepository.ComponentData data){};
}
