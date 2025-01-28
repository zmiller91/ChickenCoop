package coop.local.comms.message.parsers;

import coop.local.comms.message.MessageReceived;
import coop.shared.database.table.ComponentType;

import java.util.List;

public interface MessageParser {

    List<ParsedMessage> parse(MessageReceived message);

    static MessageParser forComponentType(ComponentType type) {
        switch(type) {
            case WEATHER:
                return new WeatherSensorMessageParser();

            default:
                return null;
        }
    }

    static String getComponentSerial(MessageReceived received) {
        String[] parts = received.getMessage().split("::");
        if(parts.length > 0) {
            return parts[0];
        }

        return null;
    }
}
