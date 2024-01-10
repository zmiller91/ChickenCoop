package coop.local.comms.message;

import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class MessageReceived implements PiMessage {

    private static final String REGEX = "^\\+RCV=(?<address>[0-9]+),(?<bytes>[0-9]+),(?<message>[ -~]+),(?<rssi>-?[0-9]+),(?<snr>-?[0-9]+)$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private final String raw;
    private int address;
    private int bytes;
    private String message;
    private int rssi;
    private int snr;

    public MessageReceived(String raw) {
        this.raw = raw;

        Matcher matcher = PATTERN.matcher(raw);
        if (matcher.matches()) {
            this.address = Integer.parseInt(matcher.group("address"));
            this.bytes = Integer.parseInt(matcher.group("bytes"));
            this.message = matcher.group("message");
            this.rssi = Integer.parseInt(matcher.group("rssi"));
            this.snr = Integer.parseInt(matcher.group("snr"));
        }
    }

    public static boolean matches(String input) {
        return PATTERN.matcher(input).matches();
    }
}
