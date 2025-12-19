package coop.local.comms.message.parsers;

import com.google.common.base.Strings;
import coop.local.comms.message.MessageReceived;
import coop.shared.database.table.ComponentType;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.stream.Stream;

public abstract class MessageParser {

    // Delimiter of the message payload
    private static final String DELIMITER = "::";

    // How many parts the message is expected to contain
    private final int messageSize;

    /**
     * @param messageSize - number of elements in a message not including the serial number
     */
    protected MessageParser(int messageSize) {
        this.messageSize = messageSize;
    }

    /**
     * Primary method that receives a payload and parses it to messages that can be understood by the broader application.
     *
     * @return
     */
    public abstract List<ParsedMessage> parse(MessageReceived message);

    /**
     * Verifies that a message has all it's parts and all those parts are well formed.
     *
     * @param message
     * @return
     */
    protected boolean isValid(MessageReceived message) {
        String[] parts = message.getMessage().split(DELIMITER);
        if(parts.length != messageSize + 1) { // +1 for serial number
            return false;
        }

        boolean containsEmptyElements = Stream.of(parts).anyMatch(Strings::isNullOrEmpty);
        if(containsEmptyElements) {
            return false;
        }

        for(int i = 1; i < parts.length; i++) {
            if(!isNumber(parts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convenience method for returning just the metric data -- i.e. everything that comes after the serial number.
     *
     * @param message
     * @return
     */
    protected String[] payload(MessageReceived message) {
        String[] payload = new String[messageSize];
        String[] parts = message.getMessage().split(DELIMITER);
        for(int i = 0; i < messageSize; i++) {
            payload[i] = parts[i + 1];
        }

        return payload;
    }

    protected String serial(MessageReceived message) {
        return message.getMessage().split(DELIMITER)[0];
    }

    private boolean isNumber(String value) {
        return NumberUtils.isParsable(value);
    }

    static double toDouble(String value) {
        return NumberUtils.toDouble(value);
    }

    public static String getComponentSerial(MessageReceived received) {
        String[] parts = received.getMessage().split(DELIMITER);
        if(parts.length > 0) {
            return parts[0];
        }

        return null;
    }

    public static MessageParser forComponentType(ComponentType type) {
        switch(type) {
            case WEATHER:
                return new WeatherSensorMessageParser();

            case SCALE:
                return new ScaleMessageParser();

            case MOISTURE:
                return new MoistureSensorParser();

            default:
                return null;
        }
    }
}
