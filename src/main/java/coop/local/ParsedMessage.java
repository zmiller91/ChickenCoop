package coop.local;

import coop.local.comms.message.MessageReceived;
import lombok.Getter;

@Getter
public class ParsedMessage {

    private final String componentId;
    private final String metric;
    private final String value;

    public ParsedMessage(String componentId, String metric, String value) {
        this.componentId = componentId;
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

        return new ParsedMessage(parts[0], parts[1], parts[2]);
    }
}
