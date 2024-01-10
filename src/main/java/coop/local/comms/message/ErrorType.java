package coop.local.comms.message;

import java.util.stream.Stream;

public enum ErrorType {

    NO_ENTER(1, "There is not “enter” or 0x0D 0x0A in the end of the AT Command."),
    NO_AT(2, "The head of AT command is not “AT” string"),
    UNKNOWN_CMD(4, "Unknown command."),
    LENGTH_MISMATCH(5, "The data to be sent does not match the actual length."),
    TX_OVER_TIMES(10, "TX is over times."),
    CRC_ERROR(12, "CRC error."),
    TX_EXCEEDS_240BTS(13, "TX data exceeds 240 bytes."),
    FLASH_MEMORY_FAILURE(14, "Failed to write flash memory."),
    UNKNOWN(15, "Unknown failure."),
    LAST_TX_NOT_COMPLETE(17, "Last TX was not completed."),
    PREAMBLE_NOT_ALLOWED(18, "Preamble value is not allowed."),
    RX_ERROR_HEADER(19, "RX failed, Header error."),
    TIME_SETTING_NOT_ALLOWED(20, "The time setting value of the \"Smart receiving power saving mode\" is not allowed.");

    private final int code;
    private final String message;

    ErrorType(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public static ErrorType findByCode(int code) {
        return Stream.of(ErrorType.values()).filter( et -> et.code == code).findFirst().orElse(null);
    }
}
