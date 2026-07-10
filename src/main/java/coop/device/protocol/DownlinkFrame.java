package coop.device.protocol;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

@Getter
public class DownlinkFrame {

    public static final String DELIMITER = "::";
    private final String id;
    private final String serialNumber;
    private final String[] payload;
    private  boolean requiresAck = false;
    private boolean requiresResources = true;

    public DownlinkFrame(String serialNumber, String... payload) {
        this(RandomStringUtils.randomAlphabetic(8), serialNumber, payload);
    }

    private DownlinkFrame(String id, String serialNumber, String... payload) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.payload = payload;
    }

    public void setRequiresAck(boolean required) {
        this.requiresAck = required;
    }

    public boolean getRequiresAck() {
        return requiresAck;
    }

    public void setRequiresResources(boolean required) {
        this.requiresResources = required;
    }

    public boolean getRequiresResources() {
        return requiresResources;
    }

    public String toString() {
        return serialNumber + DELIMITER + id + DELIMITER + String.join(DELIMITER, payload);
    }

    public static DownlinkFrame fromString(String raw) {

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Raw frame cannot be null or blank");
        }

        String[] parts = raw.split(Pattern.quote(DELIMITER));

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid frame format: " + raw);
        }

        String serialNumber = parts[0];
        String id = parts[1];

        String[] payload = Arrays.copyOfRange(parts, 2, parts.length);

        return new DownlinkFrame(id, serialNumber, payload);
    }
}
