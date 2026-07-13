package coop.shared.database.table;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@Embeddable
public class AreaComponentId implements Serializable {

    @Column(name = "AREA_ID", nullable = false)
    private String areaId;

    @Column(name = "COMPONENT_ID", nullable = false)
    private String componentId;
}
