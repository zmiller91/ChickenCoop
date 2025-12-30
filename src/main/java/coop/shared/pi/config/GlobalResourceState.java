package coop.shared.pi.config;

import coop.device.types.DeviceType;
import lombok.Data;

@Data
public class GlobalResourceState {
    private int id;
    private DeviceType deviceType;
    private int concurrency;
}
