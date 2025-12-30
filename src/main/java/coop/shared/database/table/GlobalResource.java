package coop.shared.database.table;

import coop.device.types.DeviceType;
import lombok.Data;

@Data
public class GlobalResource {
    private String id;
    private DeviceType deviceType;
    private int concurrency;
}
