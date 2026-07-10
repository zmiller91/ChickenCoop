package coop.device.protocol;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.stream.Stream;

public class UplinkFrame {

    private static final String DELIMITER = "::";
    private String serialNumber;
    private String[] payload;

    public UplinkFrame(String payload) {
        String[] split = payload.split(DELIMITER);
        if(split.length != 0) {
            this.serialNumber = split[0];
            this.payload = Arrays.copyOfRange(payload.split(DELIMITER), 1, split.length);
        }

    }

    public boolean isValid(int payloadSize) {
        return isValid(payloadSize, false);
    }

    public boolean isValid(int payloadSize, boolean useMinSize) {

        if(!useMinSize && payload.length != payloadSize) {
            return false;

        } else if(useMinSize && payload.length < payloadSize) {
            return false;
        }

        boolean containsEmptyElements = Stream.of(payload).anyMatch(Strings::isNullOrEmpty);
        return !containsEmptyElements;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getPayloadFromIdx(int idx) {
        String[] truncated = Arrays.copyOfRange(payload, idx, payload.length);
        return StringUtils.join(truncated, DELIMITER);
    }

    public String getStringAt(int idx) {
        return payload[idx];
    }

    public Double getDoubleAt(int idx) {
        if(NumberUtils.isParsable(getStringAt(idx))) {
            return Double.parseDouble(getStringAt(idx));
        }

        return null;
    }

    public Long getLongAt(int idx) {
        if(NumberUtils.isParsable(getStringAt(idx))) {
            return Long.parseLong(getStringAt(idx));
        }

        return null;
    }
}
