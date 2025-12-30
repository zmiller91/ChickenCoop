package coop.device.protocol;

import lombok.Data;

@Data
public class CommandRequestEvent implements Event {
    private final String serialNumber;
}
