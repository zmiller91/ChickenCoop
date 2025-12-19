package coop.local.comms.message.parsers;

import coop.local.comms.message.MessageReceived;

import java.util.List;

public class WeatherSensorMessageParser extends MessageParser {
    protected WeatherSensorMessageParser() {
        super(2);
    }

    @Override
    public List<ParsedMessage> parse(MessageReceived message) {

        if(!isValid(message)) {
            return null;
        }

        String serial = serial(message);
        String[] data = payload(message);

        double temperature = toDouble(data[0]) / 100.00;
        double humidity = toDouble(data[1]) / 1024;

        return List.of(
                new ParsedMessage(serial, "TEMPERATURE", String.valueOf(temperature)),
                new ParsedMessage(serial, "HUMIDITY", String.valueOf(humidity))
        );
    }
}
