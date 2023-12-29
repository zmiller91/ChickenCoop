package coop.api.auth;

import coop.database.repository.ComponentConfigRepository;
import coop.database.repository.ComponentRepository;
import coop.database.repository.ComponentSerialRepository;
import coop.database.repository.CoopRepository;
import coop.database.table.ComponentType;
import coop.database.table.CoopComponent;
import coop.database.table.ComponentSerial;
import coop.database.table.Coop;
import coop.exception.NotFound;
import coop.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/component")
public class ComponentService {

    @Autowired
    private ComponentSerialRepository componentSerialRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ComponentConfigRepository configRepository;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private AuthContext userContext;

    @PostMapping("/register")
    public RegisterComponentResponse create(@RequestBody RegisterComponentRequest request) {

        // Throw a 404 if the serial number has already been associated
        ComponentSerial serialNumber = componentSerialRepository.findById(request.serialNumber);
        if (serialNumber.getComponent() != null) {
            throw new NotFound("Entity not found.");
        }

        // Throw a 404 if the user tries to associate the component to a coop they don't own
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), request.coopId);
        if (coop == null) {
            throw new NotFound("Entity not found.");
        }

        CoopComponent component = new CoopComponent();
        component.setName(request.name);
        component.setCoop(coop);
        component.setSerial(serialNumber);
        componentRepository.persist(component);

        ComponentType componentType = component.getSerial().getComponentType();
        componentType.initialConfig(component).forEach(config -> configRepository.persist(config));

        return new RegisterComponentResponse(coop.getId(), serialNumber.getSerialNumber(), component.getComponentId());
    }

    @GetMapping("/{coopId}/list")
    public ListComponentsResponse list(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<ComponentDAO> components = coop.getComponents()
                .stream()
                .map(this::componentDao)
                .toList();

        return new ListComponentsResponse(components);
    }

    @GetMapping("/{componentId}")
    public GetComponentResponse get( @PathVariable("componentId") String componentId) {

        CoopComponent component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if(component == null) {
            throw new NotFound("Component not found.");
        }

        return new GetComponentResponse(componentDao(component));
    }

    private ComponentDAO componentDao(CoopComponent component) {

        Map<String, String> keyDisplayNames = component.getSerial().getComponentType().keyDisplayNames();

        List<ConfigDAO> config = component.getConfig()
                .stream()
                .map(c -> new ConfigDAO(c.getKey(), c.getValue(), keyDisplayNames.get(c.getKey())))
                .toList();

        return new ComponentDAO(
                component.getComponentId(),
                component.getSerial().getSerialNumber(),
                component.getName(),
                config);
    }

    public record RegisterComponentRequest(String coopId, String serialNumber, String name){}
    public record RegisterComponentResponse(String coopId, String serialNumber, String componentId){}

    public record ListComponentsResponse(List<ComponentDAO> components) {};
    public record ComponentDAO(String id, String serial, String name, List<ConfigDAO> config){}
    public record ConfigDAO(String key, String value, String name){};

    public record GetComponentResponse(ComponentDAO component){};
}
