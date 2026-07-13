package coop.shared.api.auth;

import coop.shared.database.repository.AreaComponentPortRepository;
import coop.shared.database.repository.AreaComponentRepository;
import coop.shared.database.repository.AreaRepository;
import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.table.Area;
import coop.shared.database.table.AreaComponent;
import coop.shared.database.table.AreaComponentPort;
import coop.shared.database.table.AreaComponentPortId;
import coop.shared.database.table.AreaType;
import coop.shared.database.table.Coop;
import coop.shared.database.table.component.Component;
import coop.shared.exception.BadRequest;
import coop.shared.exception.NotFound;
import coop.shared.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/areas")
public class AreaService {

    @Autowired
    private AuthContext userContext;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private AreaComponentRepository areaComponentRepository;

    @Autowired
    private AreaComponentPortRepository areaComponentPortRepository;

    @GetMapping("{coopId}/list")
    public ListAreasResponse listAreas(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        return new ListAreasResponse(
                areaRepository.findByCoop(coop).stream().map(AreaService::toDTO).toList()
        );
    }

    @PutMapping("{coopId}/add")
    public AddAreaResponse create(@PathVariable("coopId") String coopId,
                                   @RequestBody AddAreaRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Area area = new Area();
        area.setCoop(coop);
        applyRequest(coop, area, request.area());
        areaRepository.persist(area);

        return new AddAreaResponse(toDTO(area));
    }

    @PutMapping("{coopId}/update")
    public UpdateAreaResponse update(@PathVariable("coopId") String coopId,
                                      @RequestBody UpdateAreaRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Area area = areaRepository.findByIdAndCoop(coop, request.area().id());
        if (area == null) {
            throw new NotFound("Area not found.");
        }

        applyRequest(coop, area, request.area());
        areaRepository.persist(area);

        return new UpdateAreaResponse(toDTO(area));
    }

    @DeleteMapping("{coopId}/{areaId}")
    public void delete(@PathVariable("coopId") String coopId, @PathVariable("areaId") String areaId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Area area = areaRepository.findByIdAndCoop(coop, areaId);
        if (area == null) {
            throw new NotFound("Area not found.");
        }

        // Membership rows (area_component/area_component_port) cascade-delete freely at the DB level - not
        // destructive to any real hardware record. Child areas are the one thing worth refusing on, so a
        // hierarchy doesn't silently get orphaned.
        if (!areaRepository.findByParent(area).isEmpty()) {
            throw new BadRequest("This area still has child areas - remove or reassign them first.");
        }

        areaRepository.delete(area);
    }

    /**
     * Replaces the full set of areas a whole component belongs to - the simplest operation for a multi-select
     * UI to drive later (send the whole selected list, not incremental add/remove calls).
     */
    @PutMapping("{coopId}/components/{componentId}")
    public SetAreasResponse setComponentAreas(@PathVariable("coopId") String coopId,
                                               @PathVariable("componentId") String componentId,
                                               @RequestBody SetAreasRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Component component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if (component == null || !component.getCoop().equals(coop)) {
            throw new NotFound("Component not found.");
        }

        List<Area> areas = resolveAreas(coop, request.areaIds());

        areaComponentRepository.deleteByComponent(component);
        areaComponentRepository.flush();

        for (Area area : areas) {
            AreaComponent link = new AreaComponent();
            link.setArea(area);
            link.setComponent(component);
            areaComponentRepository.persist(link);
        }

        return new SetAreasResponse(areas.stream().map(AreaService::toDTO).toList());
    }

    /**
     * Same as setComponentAreas, but for a single port on a multi-port device (e.g. one of a valve's zones) -
     * independent of whatever areas the parent component itself belongs to.
     */
    @PutMapping("{coopId}/components/{componentId}/ports/{portIndex}")
    public SetAreasResponse setPortAreas(@PathVariable("coopId") String coopId,
                                          @PathVariable("componentId") String componentId,
                                          @PathVariable("portIndex") int portIndex,
                                          @RequestBody SetAreasRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Component component = componentRepository.findById(userContext.getCurrentUser(), componentId);
        if (component == null || !component.getCoop().equals(coop)) {
            throw new NotFound("Component not found.");
        }

        List<Area> areas = resolveAreas(coop, request.areaIds());

        areaComponentPortRepository.deleteByComponentAndPort(componentId, portIndex);
        areaComponentPortRepository.flush();

        for (Area area : areas) {
            AreaComponentPort link = new AreaComponentPort();
            AreaComponentPortId id = new AreaComponentPortId();
            id.setAreaId(area.getId());
            id.setComponentId(componentId);
            id.setPortIndex(portIndex);
            link.setId(id);
            link.setArea(area);
            areaComponentPortRepository.persist(link);
        }

        return new SetAreasResponse(areas.stream().map(AreaService::toDTO).toList());
    }

    private List<Area> resolveAreas(Coop coop, List<String> areaIds) {
        List<Area> areas = areaRepository.findByIdsAndCoop(coop, areaIds);
        if (areas.size() != areaIds.size()) {
            List<String> foundIds = areas.stream().map(Area::getId).toList();
            List<String> missingIds = areaIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BadRequest("Area(s) not found: " + String.join(", ", missingIds));
        }
        return areas;
    }

    private void applyRequest(Coop coop, Area area, AreaDTO dto) {
        area.setName(dto.name());
        area.setType(parseAreaType(dto.type()));

        if (dto.parentId() == null) {
            area.setParent(null);
        } else {
            Area parent = areaRepository.findByIdAndCoop(coop, dto.parentId());
            if (parent == null) {
                throw new BadRequest("Parent area not found.");
            }
            area.setParent(parent);
        }
    }

    private static AreaType parseAreaType(String type) {
        try {
            return AreaType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BadRequest("Unknown area type: " + type);
        }
    }

    public static AreaDTO toDTO(Area area) {
        return new AreaDTO(
                area.getId(),
                area.getName(),
                area.getType().name(),
                area.getParent() != null ? area.getParent().getId() : null
        );
    }

    public record ListAreasResponse(List<AreaDTO> areas) {}
    public record AreaDTO(String id, String name, String type, String parentId) {}
    public record AddAreaRequest(AreaDTO area) {}
    public record AddAreaResponse(AreaDTO area) {}
    public record UpdateAreaRequest(AreaDTO area) {}
    public record UpdateAreaResponse(AreaDTO area) {}
    public record SetAreasRequest(List<String> areaIds) {}
    public record SetAreasResponse(List<AreaDTO> areas) {}
}
