package coop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<ExceptionMessage> handleBadRequest(BadRequest ex, WebRequest request) {
        ExceptionMessage error = new ExceptionMessage("Bad Request", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFound.class)
    public ResponseEntity<ExceptionMessage> handleNotFound(BadRequest ex, WebRequest request) {
        ExceptionMessage error = new ExceptionMessage("Not Found", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
