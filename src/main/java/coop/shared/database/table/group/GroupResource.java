package coop.shared.database.table.group;

import coop.device.types.DeviceType;
import lombok.Data;

@Data
public class GroupResource {
    private String id;
    private Group group;
    private DeviceType deviceType;
    private int concurrency;
}
