package coop.local.comms.message;

import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class MessageError implements PiMessage {

    private static final String REGEX = "^\\+ERR=(?<error>[0-9]+)$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private final String raw;
    private ErrorType error;

    public MessageError(String raw) {
        this.raw = raw;

        Matcher matcher = PATTERN.matcher(raw);
        if(matcher.matches()) {
            int code = Integer.parseInt(matcher.group("error"));
            error = ErrorType.findByCode(code);
        }
    }

    public static boolean matches(String input) {
        return PATTERN.matcher(input).matches();
    }
}
