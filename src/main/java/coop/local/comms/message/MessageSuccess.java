package coop.local.comms.message;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class MessageSuccess implements PiMessage {

    private static final String REGEX = "^\\+OK$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private final String raw;
    public MessageSuccess(String raw) {
        this.raw = raw;
    }

    public static boolean matches(String input) {
        return PATTERN.matcher(input).matches();
    }
}
