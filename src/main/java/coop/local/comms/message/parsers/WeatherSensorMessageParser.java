package coop.local.comms.message.parsers;

import com.google.common.base.Strings;
import coop.local.comms.message.MessageReceived;

import java.util.List;
import java.util.stream.Stream;

public class WeatherSensorMessageParser implements MessageParser {
    @Override
    public List<ParsedMessage> parse(MessageReceived message) {

        String[] parts = message.getMessage().split("::");
        if(parts.length != 5) {
            return null;
        }

        boolean containsEmptyElements = Stream.of(parts).anyMatch(Strings::isNullOrEmpty);
        if(containsEmptyElements) {
            return null;
        }

        String serial = parts[0];
        return List.of(
                new ParsedMessage(serial, "TEMPERATURE", parts[1]),
                new ParsedMessage(serial, "HUMIDITY", parts[2]),
                new ParsedMessage(serial, "PRESSURE", parts[3]),
                new ParsedMessage(serial, "BATTERY", parts[4])
        );
    }
}
