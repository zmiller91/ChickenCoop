package coop.exception;

import lombok.Data;

@Data
public class ExceptionMessage {

    private String message;
    private String details;

    public ExceptionMessage(String message, String details) {
        this.message = message;
        this.details = details;
    }
}