package coop.local;

import com.google.common.base.Strings;
import coop.local.comms.message.MessageReceived;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
public class ParsedMessage {

    private final String componentSerialNumber;
    private final String metric;
    private final String value;

    public ParsedMessage(String componentSerialNumber, String metric, String value) {
        this.componentSerialNumber = componentSerialNumber;
        this.metric = metric;
        this.value = value;
    }

    public Double getValueAsDouble() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isValueDoubleType() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static ParsedMessage parse(MessageReceived received) {

        String[] parts = received.getMessage().split("::");
        if(parts.length != 3) {
            return null;
        }

        boolean containsEmptyElements = Stream.of(parts).anyMatch(Strings::isNullOrEmpty);
        if(containsEmptyElements) {
            return null;
        }

        return new ParsedMessage(parts[0], parts[1], parts[2]);
    }
}
