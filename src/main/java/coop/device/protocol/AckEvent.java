package coop.device.protocol;

import lombok.Data;

@Data
public class AckEvent implements Event {
    private String serialNumber;
    private String messageId;
}
