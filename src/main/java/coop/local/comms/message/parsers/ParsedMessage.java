package coop.local.comms.message.parsers;

import lombok.Getter;

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
}
