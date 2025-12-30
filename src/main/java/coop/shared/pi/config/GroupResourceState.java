package coop.shared.pi.config;

import coop.device.types.DeviceType;
import lombok.Data;

@Data
public class GroupResourceState {
    private String id;
    private String groupId;
    private DeviceType deviceType;
    private int concurrency;
}
