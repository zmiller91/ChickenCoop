package coop.local.comms.message.parsers;

import coop.local.comms.message.MessageReceived;

import java.util.List;

public class MoistureSensorParser extends MessageParser {

    protected MoistureSensorParser() {
        super(1);
    }

    @Override
    public List<ParsedMessage> parse(MessageReceived message) {

        if(!isValid(message)) {
            return null;
        }

        String serial = serial(message);
        String[] data = payload(message);

        double percent = toDouble(data[0]) / 100.00;

        return List.of(
                new ParsedMessage(serial, "MOISTURE_PERCENT", String.valueOf(percent))
        );
    }
}
