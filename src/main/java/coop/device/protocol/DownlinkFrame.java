package coop.device.protocol;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

@Getter
public class DownlinkFrame {

    private static final String DELIMITER = "::";
    private final String id;
    private final String serialNumber;
    private final String[] payload;
    private  boolean requiresAck = false;

    public DownlinkFrame(String serialNumber, String... payload) {
        this.id = RandomStringUtils.randomAlphabetic(8);
        this.serialNumber = serialNumber;
        this.payload = payload;
    }

    public void setRequiresAck(boolean required) {
        this.requiresAck = required;
    }

    public boolean getRequiresAck() {
        return requiresAck;
    }

    public String toString() {
        return serialNumber + DELIMITER + id + DELIMITER + String.join(DELIMITER, payload);
    }
}
