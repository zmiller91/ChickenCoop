package coop.shared.database.table;

import coop.shared.database.table.component.Component;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Many-to-many membership row: a whole component (a sensor, or a multi-port device broadly) belongs to an
 * Area. See AreaComponentPort for per-port membership on multi-port devices.
 */
@Getter
@Setter
@Entity
@Table(name = "area_component")
public class AreaComponent {

    @EmbeddedId
    private AreaComponentId id = new AreaComponentId();

    @MapsId("areaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AREA_ID", nullable = false)
    private Area area;

    @MapsId("componentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMPONENT_ID", nullable = false)
    private Component component;
}
