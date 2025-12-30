package coop.shared.api.auth;

import coop.device.ConfigKey;
import coop.shared.database.repository.ComponentConfigRepository;
import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.ComponentSerialRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.table.*;
import coop.shared.database.table.component.ComponentConfig;
import coop.shared.database.table.component.ComponentSerial;
import coop.device.DeviceType;
import coop.shared.database.table.component.Component;
import coop.shared.exception.NotFound;
import coop.shared.pi.StateFactory;
import coop.shared.pi.StateProvider;
import coop.shared.pi.config.CoopState;
import coop.shared.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    @Autowired
    private StateProvider stateProvider;

    @Autowired
    private StateFactory stateFactory;

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

        Component component = new Component();
        component.setName(request.name);
        component.setCoop(coop);
        component.setSerial(serialNumber);
        componentRepository.persist(component);

        DeviceType deviceList = component.getSerial().getDeviceType();
        initialConfig(component).forEach(config -> configRepository.persist(config));

        componentRepository.flush();
        configRepository.flush();
        componentRepository.refresh(component);

        CoopState state = stateFactory.forCoop(component.getCoop());
        stateProvider.put(state);

        return new RegisterComponentResponse(coop.getId(), serialNumber.getSerialNumber(), component.getComponentId());
    }

    private List<ComponentConfig> initialConfig(Component component) {
        return Stream.of(component.getSerial().getDeviceType().getDevice().getConfig()).map(c -> {
            ComponentConfig cc = new ComponentConfig();
            cc.setComponent(component);
            cc.setKey(c.getKey());
            cc.setValue("");
            return cc;
        }).toList();
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

        Component component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if(component == null) {
            throw new NotFound("Component not found.");
        }

        return new GetComponentResponse(componentDao(component));
    }

    @PostMapping("/{componentId}")
    public PostComponentResponse post(@RequestBody PostComponentRequest request) {

        Component component = componentRepository.findById(userContext.getCurrentUser(), request.component.id);
        if(component == null) {
            throw new NotFound("Component not found.");
        }

        request.component.config.forEach(c -> configRepository.save(component, c.key, c.value));

        Coop coop = component.getCoop();
        coopRepository.refresh(coop);

        CoopState state = stateFactory.forCoop(component.getCoop());
        stateProvider.put(state);

        return new PostComponentResponse();
    }

    private ComponentDAO componentDao(Component component) {

        Map<String, String> keyDisplayNames = keyDisplayNames(component);

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

    private Map<String, String> keyDisplayNames(Component component) {
        Map<String, String> map = new HashMap<>();
        for(ConfigKey key : component.getSerial().getDeviceType().getDevice().getConfig()) {
            map.put(key.getKey(), key.getDisplayName());
        }

        return map;
    }

    public record RegisterComponentRequest(String coopId, String serialNumber, String name){}
    public record RegisterComponentResponse(String coopId, String serialNumber, String componentId){}

    public record ListComponentsResponse(List<ComponentDAO> components) {};
    public record ComponentDAO(String id, String serial, String name, List<ConfigDAO> config){}
    public record ConfigDAO(String key, String value, String name){};

    public record GetComponentResponse(ComponentDAO component){};

    public record PostComponentRequest(ComponentDAO component){};
    public record PostComponentResponse(){}
}
