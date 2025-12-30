package coop.device.protocol.event;

import lombok.Data;

@Data
public class AckEvent implements Event {
    private String serialNumber;
    private String messageId;
}
