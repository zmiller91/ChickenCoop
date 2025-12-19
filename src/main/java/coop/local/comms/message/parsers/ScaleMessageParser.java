package coop.local.comms.message.parsers;

import coop.local.comms.message.MessageReceived;

import java.util.List;

public class ScaleMessageParser extends MessageParser {

    protected ScaleMessageParser() {
        super(1);
    }

    @Override
    public List<ParsedMessage> parse(MessageReceived message) {

        if(!isValid(message)) {
            return null;
        }

        String serial = serial(message);
        String[] data = payload(message);
        return List.of(new ParsedMessage(serial, "WEIGHT", data[0]));
    }
}
