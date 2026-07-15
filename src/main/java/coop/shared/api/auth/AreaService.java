package coop.shared.api.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import coop.shared.database.repository.AreaComponentPortRepository;
import coop.shared.database.repository.AreaComponentRepository;
import coop.shared.database.repository.AreaRepository;
import coop.shared.database.repository.ComponentRepository;
import coop.shared.database.repository.ContactRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.MetricRepository;
import coop.shared.database.repository.PortActionLogRepository;
import coop.shared.database.table.Area;
import coop.shared.database.table.AreaComponent;
import coop.shared.database.table.AreaComponentPort;
import coop.shared.database.table.AreaComponentPortId;
import coop.shared.database.table.AreaType;
import coop.shared.database.table.Contact;
import coop.shared.database.table.Coop;
import coop.shared.database.table.component.Component;
import coop.shared.database.table.component.PortActionLogEntry;
import coop.shared.exception.BadRequest;
import coop.shared.exception.NotFound;
import coop.shared.security.AuthContext;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/areas")
@Log4j2
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

    @Autowired
    private PortActionLogRepository portActionLogRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private SesClient ses;

    @Autowired
    private ObjectMapper objectMapper;

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

        Component component = componentRepository.findByCoopAndId(coop, componentId);
        if (component == null) {
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
     * Same as setComponentAreas, but for every component in one request/transaction - lets a caller with N
     * components to update (e.g. the Edit page's membership save) avoid N separate calls, which previously had
     * to be sequenced client-side to dodge a MySQL/InnoDB deadlock (concurrent transactions all inserting into
     * area_component against the same Area row via implicit FK locking). A single transaction serializes its
     * own writes, so the deadlock doesn't apply here regardless of assignment order.
     */
    @PutMapping("{coopId}/components/bulk")
    public BulkSetAreasResponse setComponentAreasBulk(@PathVariable("coopId") String coopId,
                                                        @RequestBody BulkSetAreasRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<BulkAreaAssignmentResult> results = new ArrayList<>();

        for (ComponentAreaAssignment assignment : request.assignments()) {
            Component component = componentRepository.findByCoopAndId(coop, assignment.componentId());
            if (component == null) {
                throw new NotFound("Component not found: " + assignment.componentId());
            }

            List<Area> areas = resolveAreas(coop, assignment.areaIds());

            areaComponentRepository.deleteByComponent(component);
            areaComponentRepository.flush();

            for (Area area : areas) {
                AreaComponent link = new AreaComponent();
                link.setArea(area);
                link.setComponent(component);
                areaComponentRepository.persist(link);
            }

            results.add(new BulkAreaAssignmentResult(
                    component.getComponentId(),
                    areas.stream().map(AreaService::toDTO).toList()));
        }

        return new BulkSetAreasResponse(results);
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

        Component component = componentRepository.findByCoopAndId(coop, componentId);
        if (component == null) {
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

    /**
     * Same as setComponentAreasBulk, but for ports - lets a caller with N port assignments to update (e.g.
     * a Garden Bed's edit page saving its irrigation-zone associations) avoid N separate calls for the same
     * deadlock-avoidance reason.
     */
    @PutMapping("{coopId}/components/ports/bulk")
    public BulkSetPortAreasResponse setPortAreasBulk(@PathVariable("coopId") String coopId,
                                                       @RequestBody BulkSetPortAreasRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        List<BulkPortAreaAssignmentResult> results = new ArrayList<>();

        for (PortAreaAssignment assignment : request.assignments()) {
            Component component = componentRepository.findByCoopAndId(coop, assignment.componentId());
            if (component == null) {
                throw new NotFound("Component not found: " + assignment.componentId());
            }

            List<Area> areas = resolveAreas(coop, assignment.areaIds());

            areaComponentPortRepository.deleteByComponentAndPort(assignment.componentId(), assignment.portIndex());
            areaComponentPortRepository.flush();

            for (Area area : areas) {
                AreaComponentPort link = new AreaComponentPort();
                AreaComponentPortId id = new AreaComponentPortId();
                id.setAreaId(area.getId());
                id.setComponentId(assignment.componentId());
                id.setPortIndex(assignment.portIndex());
                link.setId(id);
                link.setArea(area);
                areaComponentPortRepository.persist(link);
            }

            results.add(new BulkPortAreaAssignmentResult(
                    assignment.componentId(),
                    assignment.portIndex(),
                    areas.stream().map(AreaService::toDTO).toList()));
        }

        return new BulkSetPortAreasResponse(results);
    }

    @GetMapping("{coopId}/{areaId}/activity")
    public AreaActivityResponse activity(@PathVariable("coopId") String coopId, @PathVariable("areaId") String areaId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Area area = areaRepository.findByIdAndCoop(coop, areaId);
        if (area == null) {
            throw new NotFound("Area not found.");
        }

        List<Component> components = areaComponentRepository.findByArea(area).stream()
                .map(AreaComponent::getComponent)
                .toList();

        List<PortActionLogEntry> componentEntries = portActionLogRepository.findRecentByComponents(components, 20);

        // A device's individual port (e.g. one of a valve's zones) can be linked to an area on its own,
        // independent of whether the whole component is a member - gathered separately since it isn't
        // covered by the AreaComponent-based lookup above.
        List<PortActionLogEntry> portEntries = areaComponentPortRepository.findByArea(area).stream()
                .flatMap(link -> {
                    Component portComponent = componentRepository.findByCoopAndId(coop, link.getId().getComponentId());
                    return portComponent == null
                            ? Stream.<PortActionLogEntry>empty()
                            : portActionLogRepository.findRecent(portComponent, link.getId().getPortIndex(), 20).stream();
                })
                .toList();

        List<PortActionLogEntry> entries = Stream.concat(componentEntries.stream(), portEntries.stream())
                .sorted(Comparator.comparingLong(PortActionLogEntry::getCreatedAt).reversed())
                .limit(30)
                .toList();

        return new AreaActivityResponse(entries.stream()
                .map(e -> new ActivityEntryDAO(
                        e.getComponent().getComponentId(),
                        e.getComponent().getName(),
                        e.getPortIndex(),
                        e.getActionKey(),
                        e.getSource() != null ? e.getSource().name() : null,
                        e.getStatus().name(),
                        e.getCreatedAt()))
                .toList());
    }

    /**
     * Emails a structured export of an area's metrics and device events, bucketed to the hour over a
     * trailing window (7 days by default), to a contact's email address. Scope is this area plus its
     * ancestors - a Garden Bed's export also carries whatever the enclosing Garden covers, but never a
     * sibling bed's or any other descendant's data, per the acceptance criteria.
     */
    @PostMapping("{coopId}/{areaId}/export")
    public void export(@PathVariable("coopId") String coopId,
                        @PathVariable("areaId") String areaId,
                        @RequestBody ExportRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Area area = areaRepository.findByIdAndCoop(coop, areaId);
        if (area == null) {
            throw new NotFound("Area not found.");
        }

        Contact contact = contactRepository.findByIdAndCoop(coop, request.contactId());
        if (contact == null) {
            throw new NotFound("Contact not found.");
        }
        if (StringUtils.isEmpty(contact.getEmail())) {
            throw new BadRequest("Contact has no email address.");
        }

        int days = request.days() != null ? request.days() : 7;

        List<Area> lineage = new ArrayList<>();
        for (Area current = area; current != null; current = current.getParent()) {
            lineage.add(current);
        }

        Set<Component> fullMemberComponents = new LinkedHashSet<>();
        Map<String, Set<Integer>> portMemberships = new LinkedHashMap<>();

        for (Area a : lineage) {
            areaComponentRepository.findByArea(a).forEach(link -> fullMemberComponents.add(link.getComponent()));

            for (AreaComponentPort link : areaComponentPortRepository.findByArea(a)) {
                Component component = componentRepository.findByCoopAndId(coop, link.getId().getComponentId());
                if (component != null) {
                    portMemberships.computeIfAbsent(component.getComponentId(), k -> new LinkedHashSet<>())
                            .add(link.getId().getPortIndex());
                }
            }
        }

        long sinceEpoch = System.currentTimeMillis() - Duration.ofDays(days).toMillis();

        // Metrics have no port dimension (the `metrics` table only keys on COMPONENT_ID + METRIC), so a
        // port-linked-only component's readings can't be attributed to the specific port an area is
        // actually linked to the way its events can (port_action_log does have PORT_INDEX). Left out of
        // the metrics side of the export for now rather than guess - revisit if that granularity becomes
        // available, or if a coarser "whole component" reading turns out to be wanted anyway.
        List<MetricRepository.HourlyMetricRow> metricRows =
                metricRepository.findHourlyByComponents(coop, new ArrayList<>(fullMemberComponents), sinceEpoch);

        // A component that's a full member of an ANCESTOR area (e.g. an irrigation controller tied to the
        // whole Garden) can have individual ports separately linked to sibling areas (e.g. a different
        // Garden Bed's own zone). Without this, "whole component" event lookup below would pull in every
        // sibling's port activity too, just because they happen to share the same device further up the
        // tree. Exclude any port that's specifically assigned to an area outside this export's own lineage.
        Set<String> lineageAreaIds = new LinkedHashSet<>();
        lineage.forEach(a -> lineageAreaIds.add(a.getId()));

        Map<String, Set<Integer>> excludedPortsByComponent = new LinkedHashMap<>();
        for (Component component : fullMemberComponents) {
            Set<Integer> excluded = new LinkedHashSet<>();
            for (AreaComponentPort link : areaComponentPortRepository.findByComponentId(component.getComponentId())) {
                if (!lineageAreaIds.contains(link.getArea().getId())) {
                    excluded.add(link.getId().getPortIndex());
                }
            }
            if (!excluded.isEmpty()) {
                excludedPortsByComponent.put(component.getComponentId(), excluded);
            }
        }

        List<PortActionLogEntry> events = new ArrayList<>(
                portActionLogRepository.findSinceByComponents(new ArrayList<>(fullMemberComponents), sinceEpoch).stream()
                        .filter(e -> {
                            Set<Integer> excluded = excludedPortsByComponent.get(e.getComponent().getComponentId());
                            return excluded == null || !excluded.contains(e.getPortIndex());
                        })
                        .toList());

        for (Map.Entry<String, Set<Integer>> entry : portMemberships.entrySet()) {
            Component component = componentRepository.findByCoopAndId(coop, entry.getKey());
            // A full-member component's whole log is already covered above - only pull port-scoped
            // entries for components that are linked at the port level only.
            if (component == null || fullMemberComponents.contains(component)) {
                continue;
            }
            for (int portIndex : entry.getValue()) {
                events.addAll(portActionLogRepository.findSince(component, portIndex, sinceEpoch));
            }
        }

        DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("yyyyMMddHH");
        DateTimeFormatter isoFormat = DateTimeFormatter.ISO_DATE_TIME;
        ZoneId zone = ZoneId.of("America/Chicago");

        Map<String, Map<String, Map<String, Double>>> metricsByHour = new TreeMap<>();
        for (MetricRepository.HourlyMetricRow row : metricRows) {
            metricsByHour
                    .computeIfAbsent(row.getHour(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(row.getComponentId(), k -> new LinkedHashMap<>())
                    .put(row.getMetric(), row.getValue());
        }

        Map<String, List<ActivityEntryDAO>> eventsByHour = new TreeMap<>();
        for (PortActionLogEntry e : events) {
            String hourKey = Instant.ofEpochMilli(e.getCreatedAt()).atZone(zone).format(hourFormat);
            eventsByHour.computeIfAbsent(hourKey, k -> new ArrayList<>())
                    .add(new ActivityEntryDAO(
                            e.getComponent().getComponentId(),
                            e.getComponent().getName(),
                            e.getPortIndex(),
                            e.getActionKey(),
                            e.getSource() != null ? e.getSource().name() : null,
                            e.getStatus().name(),
                            e.getCreatedAt()));
        }

        Set<String> hourKeys = new TreeSet<>();
        hourKeys.addAll(metricsByHour.keySet());
        hourKeys.addAll(eventsByHour.keySet());

        List<AreaExportRow> rows = hourKeys.stream()
                .map(hourKey -> new AreaExportRow(
                        LocalDateTime.parse(hourKey, hourFormat).format(isoFormat),
                        metricsByHour.getOrDefault(hourKey, Map.of()),
                        eventsByHour.getOrDefault(hourKey, List.of())))
                .toList();

        String json;
        try {
            json = objectMapper.writeValueAsString(new AreaExportResponse(rows));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize area export.", e);
        }

        // The full JSON dump used to go straight in the email body, which reads as spam-bait to most mail
        // providers (a wall of raw JSON with no readable text). It goes as an attachment instead, with a
        // short human-readable body describing what was requested and when.
        DateTimeFormatter requestedAtFormat = DateTimeFormatter.ofPattern("h:mm a 'on' MMMM d, yyyy");
        String requestedAt = Instant.now().atZone(zone).format(requestedAtFormat);
        String bodyText = "Log request: " + area.getName() + " at " + requestedAt + ".";
        String attachmentFilename = slug(area.getName()) + "-logs.json";

        SendRawEmailResponse response = sendExportEmail(contact.getEmail(), area.getName() + " Logs", bodyText, attachmentFilename, json);
        log.info("Sent area export email, message id: " + response.messageId());
    }

    /**
     * SES's simple SendEmail API has no concept of attachments - only SendRawEmail (a hand-built MIME
     * message) supports them, hence building this by hand rather than pulling in a mail library just for
     * this one multipart message.
     */
    private SendRawEmailResponse sendExportEmail(String toAddress, String subject, String bodyText,
                                                  String attachmentFilename, String attachmentJson) {
        String boundary = "----=_Part_" + UUID.randomUUID();
        String base64Attachment = Base64.getEncoder().encodeToString(attachmentJson.getBytes(StandardCharsets.UTF_8));

        String raw = "From: alerts@gnomelyhq.com\r\n" +
                "To: " + toAddress + "\r\n" +
                "Subject: " + subject + "\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n" +
                "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Type: text/plain; charset=\"UTF-8\"\r\n" +
                "\r\n" +
                bodyText + "\r\n" +
                "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Type: application/json; name=\"" + attachmentFilename + "\"\r\n" +
                "Content-Disposition: attachment; filename=\"" + attachmentFilename + "\"\r\n" +
                "Content-Transfer-Encoding: base64\r\n" +
                "\r\n" +
                base64Attachment + "\r\n" +
                "--" + boundary + "--\r\n";

        SendRawEmailRequest emailRequest = SendRawEmailRequest.builder()
                .rawMessage(RawMessage.builder().data(SdkBytes.fromUtf8String(raw)).build())
                .build();

        return ses.sendRawEmail(emailRequest);
    }

    private static String slug(String value) {
        String slug = value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "area" : slug;
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
    public record ActivityEntryDAO(String componentId, String componentName, int portIndex, String actionKey, String source, String status, long createdAt) {}
    public record AreaActivityResponse(List<ActivityEntryDAO> entries) {}
    public record AreaExportRow(String timestamp, Map<String, Map<String, Double>> metrics, List<ActivityEntryDAO> events) {}
    public record AreaExportResponse(List<AreaExportRow> rows) {}
    public record ExportRequest(String contactId, Integer days) {}
    public record ComponentAreaAssignment(String componentId, List<String> areaIds) {}
    public record BulkSetAreasRequest(List<ComponentAreaAssignment> assignments) {}
    public record BulkAreaAssignmentResult(String componentId, List<AreaDTO> areas) {}
    public record BulkSetAreasResponse(List<BulkAreaAssignmentResult> results) {}
    public record PortAreaAssignment(String componentId, int portIndex, List<String> areaIds) {}
    public record BulkSetPortAreasRequest(List<PortAreaAssignment> assignments) {}
    public record BulkPortAreaAssignmentResult(String componentId, int portIndex, List<AreaDTO> areas) {}
    public record BulkSetPortAreasResponse(List<BulkPortAreaAssignmentResult> results) {}
}
