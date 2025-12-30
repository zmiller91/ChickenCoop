package coop.device.protocol.event;

import lombok.Data;

@Data
public class CommandRequestEvent implements Event {
    private final String serialNumber;
}
