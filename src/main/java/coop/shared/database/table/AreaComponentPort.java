package coop.shared.database.table;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Many-to-many membership row: a single port on a multi-port device (a valve's zone) belongs to an Area,
 * independent of whatever the parent component's own AreaComponent memberships are. componentId/portIndex
 * are plain values here rather than a mapped relation to ComponentPort - callers already have both in hand
 * (they came from the component/port being assigned), no need for object-graph navigation back through this
 * row for how it's used today.
 */
@Getter
@Setter
@Entity
@Table(name = "area_component_port")
public class AreaComponentPort {

    @EmbeddedId
    private AreaComponentPortId id = new AreaComponentPortId();

    @MapsId("areaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AREA_ID", nullable = false)
    private Area area;
}
