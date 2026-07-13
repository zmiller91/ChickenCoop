package coop.shared.api.auth;

import coop.device.Actuator;
import coop.device.ConfigKey;
import coop.shared.database.repository.ComponentConfigRepository;
import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.ComponentSerialRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.ComponentPortRepository;
import coop.shared.database.repository.PortActionLogRepository;
import coop.shared.database.repository.PortConfigRepository;
import coop.shared.database.table.*;
import coop.shared.database.table.component.ComponentConfig;
import coop.shared.database.table.component.ComponentSerial;
import coop.device.types.DeviceType;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.component.ComponentPort;
import coop.shared.database.table.component.PortActionLogEntry;
import coop.shared.database.table.component.PortActionSource;
import coop.shared.database.table.component.PortActionStatus;
import coop.shared.database.table.component.PortConfig;
import coop.device.types.valve.ValveActuator;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
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
    private PortActionLogRepository portActionLogRepository;

    @Autowired
    private ComponentPortRepository portRepository;

    @Autowired
    private PortConfigRepository portConfigRepository;

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
        initialPorts(component).forEach(port -> portRepository.persist(port));
        initialPortConfig(component).forEach(config -> portConfigRepository.persist(config));

        componentRepository.flush();
        configRepository.flush();
        portRepository.flush();
        portConfigRepository.flush();
        componentRepository.refresh(component);

        CoopState state = stateFactory.forCoop(component.getCoop());
        stateProvider.put(state);

        return new RegisterComponentResponse(coop.getId(), serialNumber.getSerialNumber(), component.getComponentId());
    }

    /**
     * Weather forecast components are virtual - there's no physical device to hand the user a pre-printed
     * serial number for, so unlike /register this generates its own (scoped to the coop, since serial numbers
     * are a single global namespace and every coop needs its own component/serial pair - they can't share
     * one). One per coop for now; free while the forecast data stays cheap/simple - if it grows into
     * something metered later, this is the endpoint that would gain that check.
     */
    @PostMapping("/{coopId}/weather-forecast")
    public RegisterComponentResponse registerWeatherForecast(@PathVariable("coopId") String coopId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        boolean alreadyExists = coop.getComponents().stream()
                .anyMatch(c -> c.getSerial().getDeviceType() == DeviceType.WEATHER_FORECAST);
        if (alreadyExists) {
            throw new BadRequest("This coop already has a weather forecast component.");
        }

        // SERIAL_NUMBER is varchar(32), so a random UUID with the dashes stripped fits exactly. Deliberately
        // not derived from coopId (e.g. reusing it directly) - that would permanently tie one serial to one
        // coop with no room for a second down the line. Nothing associates a serial back to its coop by
        // reading the string itself; join through the components table for that.
        ComponentSerial serial = new ComponentSerial();
        serial.setSerialNumber(UUID.randomUUID().toString().replace("-", ""));
        serial.setDeviceType(DeviceType.WEATHER_FORECAST);
        componentSerialRepository.persist(serial);

        Component component = new Component();
        component.setName("Weather Forecast");
        component.setCoop(coop);
        component.setSerial(serial);
        componentRepository.persist(component);

        initialConfig(component).forEach(config -> configRepository.persist(config));

        componentRepository.flush();
        configRepository.flush();
        componentRepository.refresh(component);

        CoopState state = stateFactory.forCoop(coop);
        stateProvider.put(state);

        return new RegisterComponentResponse(coop.getId(), serial.getSerialNumber(), component.getComponentId());
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

    private List<ComponentPort> initialPorts(Component component) {
        if(component.getSerial().getDeviceType() != DeviceType.VALVE) {
            return List.of();
        }

        return IntStream.range(0, ValveActuator.PORT_COUNT)
                .mapToObj(i -> {
                    ComponentPort port = new ComponentPort();
                    port.setComponent(component);
                    port.setPortIndex(i);
                    port.setName(defaultPortName(component.getSerial().getDeviceType(), i));
                    return port;
                }).toList();
    }

    private String defaultPortName(DeviceType type, int index) {
        return switch (type) {
            case VALVE -> "Zone " + (index + 1);
            default -> "Port " + (index + 1);
        };
    }

    private List<PortConfig> initialPortConfig(Component component) {
        ConfigKey[] portKeys = component.getSerial().getDeviceType().getDevice().getPortConfig();
        if(portKeys.length == 0) {
            return List.of();
        }

        return IntStream.range(0, ValveActuator.PORT_COUNT)
                .boxed()
                .flatMap(i -> Stream.of(portKeys).map(key -> {
                    PortConfig pc = new PortConfig();
                    pc.setComponent(component);
                    pc.setPortIndex(i);
                    pc.setKey(key.getKey());
                    pc.setValue("");
                    return pc;
                }))
                .toList();
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

    @PostMapping("/{componentId}/manual")
    public ManualCommandResponse manual(@PathVariable("componentId") String componentId, @RequestBody ManualCommandRequest request) {

        Component component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if(component == null) {
            throw new NotFound("Component not found.");
        }

        if(!(component.getSerial().getDeviceType().getDevice() instanceof Actuator actuator)) {
            throw new BadRequest("Component is not an actuator.");
        }

        // Params are fully resolved server-side (matches how rule actions are already baked ahead of time) -
        // the Pi doesn't need to look anything up, it just executes what it's told.
        Map<String, String> params = new HashMap<>();
        params.put("zone", request.zone());
        if("TURN_ON".equals(request.actionKey())) {
            PortConfig duration = portConfigRepository.findByKey(component, Integer.parseInt(request.zone()), "default_duration");
            params.put("duration", duration != null ? duration.getValue() : null);
        }

        if(!actuator.validateCommand(request.actionKey(), params)) {
            throw new BadRequest("Invalid command.");
        }

        PortActionLogEntry logEntry = new PortActionLogEntry();
        logEntry.setComponent(component);
        logEntry.setPortIndex(Integer.parseInt(request.zone()));
        logEntry.setActionKey(request.actionKey());
        logEntry.setSource(PortActionSource.MANUAL);
        logEntry.setStatus(PortActionStatus.REQUESTED);
        logEntry.setCreatedAt(System.currentTimeMillis());
        portActionLogRepository.persist(logEntry);

        stateProvider.sendCommand(component.getCoop(), component.getComponentId(), request.actionKey(), params);

        return new ManualCommandResponse();
    }

    @GetMapping("/{componentId}/ports/{index}/log")
    public PortLogResponse portLog(@PathVariable("componentId") String componentId, @PathVariable("index") int index) {

        Component component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if(component == null) {
            throw new NotFound("Component not found.");
        }

        List<PortLogEntryDAO> entries = portActionLogRepository.findRecent(component, index, 50)
                .stream()
                .map(e -> new PortLogEntryDAO(
                        e.getActionKey(),
                        e.getSource() != null ? e.getSource().name() : null,
                        e.getStatus().name(),
                        e.getCreatedAt()))
                .toList();

        return new PortLogResponse(entries);
    }

    @PostMapping("/{componentId}/ports")
    public PostPortsResponse ports(@PathVariable("componentId") String componentId, @RequestBody PostPortsRequest request) {

        Component component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if(component == null) {
            throw new NotFound("Component not found.");
        }

        request.ports().forEach(p -> {
            portRepository.save(component, p.index(), p.name());
            if(p.config() != null) {
                p.config().forEach(c -> portConfigRepository.save(component, p.index(), c.key(), c.value()));
            }
        });

        return new PostPortsResponse();
    }

    private ComponentDAO componentDao(Component component) {

        Map<String, String> keyDisplayNames = keyDisplayNames(component);

        // Only surface config keys the device type still declares - drops orphaned rows left behind when a
        // key moves from component-level to port-level (or is retired) without a schema migration.
        List<ConfigDAO> config = component.getConfig()
                .stream()
                .filter(c -> keyDisplayNames.containsKey(c.getKey()))
                .map(c -> new ConfigDAO(c.getKey(), c.getValue(), keyDisplayNames.get(c.getKey())))
                .toList();

        Map<String, String> portKeyDisplayNames = portKeyDisplayNames(component);

        Map<Integer, String> portStates = new HashMap<>();
        for(PortActionLogEntry entry : portActionLogRepository.findLatestComplete(component)) {
            portStates.put(entry.getPortIndex(), "TURN_ON".equals(entry.getActionKey()) ? "ON" : "OFF");
        }

        List<PortDAO> ports = portRepository.findByComponent(component)
                .stream()
                .map(p -> {
                    List<ConfigDAO> portConfig = portConfigRepository.findByPort(component, p.getPortIndex())
                            .stream()
                            .filter(c -> portKeyDisplayNames.containsKey(c.getKey()))
                            .map(c -> new ConfigDAO(c.getKey(), c.getValue(), portKeyDisplayNames.get(c.getKey())))
                            .toList();
                    return new PortDAO(p.getPortIndex(), p.getName(), portConfig, portStates.get(p.getPortIndex()));
                })
                .toList();

        return new ComponentDAO(
                component.getComponentId(),
                component.getSerial().getSerialNumber(),
                component.getName(),
                component.getSerial().getDeviceType().name(),
                config,
                ports);
    }

    private Map<String, String> keyDisplayNames(Component component) {
        Map<String, String> map = new HashMap<>();
        for(ConfigKey key : component.getSerial().getDeviceType().getDevice().getConfig()) {
            map.put(key.getKey(), key.getDisplayName());
        }

        return map;
    }

    private Map<String, String> portKeyDisplayNames(Component component) {
        Map<String, String> map = new HashMap<>();
        for(ConfigKey key : component.getSerial().getDeviceType().getDevice().getPortConfig()) {
            map.put(key.getKey(), key.getDisplayName());
        }

        return map;
    }

    public record RegisterComponentRequest(String coopId, String serialNumber, String name){}
    public record RegisterComponentResponse(String coopId, String serialNumber, String componentId){}

    public record ListComponentsResponse(List<ComponentDAO> components) {};
    public record ComponentDAO(String id, String serial, String name, String type, List<ConfigDAO> config, List<PortDAO> ports){}
    public record ConfigDAO(String key, String value, String name){};
    public record PortDAO(int index, String name, List<ConfigDAO> config, String state){};

    public record GetComponentResponse(ComponentDAO component){};

    public record PostComponentRequest(ComponentDAO component){};
    public record PostComponentResponse(){}

    public record ManualCommandRequest(String actionKey, String zone){}
    public record ManualCommandResponse(){}

    public record PostPortsRequest(List<PortDAO> ports){}
    public record PostPortsResponse(){}

    public record PortLogEntryDAO(String actionKey, String source, String status, long createdAt){}
    public record PortLogResponse(List<PortLogEntryDAO> entries){}
}
