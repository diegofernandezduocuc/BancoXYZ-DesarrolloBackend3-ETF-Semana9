package cl.duoc.bancoxyz.mobile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class MobileExceptionHandler {
    @ExceptionHandler(BackendUnavailableException.class)
    ResponseEntity<ApiError> unavailable(BackendUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(
                LocalDateTime.now(), 503, "Service Unavailable", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(
                LocalDateTime.now(), ex.getStatusCode().value(), "Request Error",
                ex.getReason() != null ? ex.getReason() : "Solicitud no válida"));
    }
}
