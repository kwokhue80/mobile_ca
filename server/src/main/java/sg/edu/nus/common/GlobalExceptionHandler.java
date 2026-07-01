package sg.edu.nus.common;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
*   AUTHOR:     Amelia
*   PURPOSE:    Handle errors on global level
*/
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle field validation on registration
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException e
    ) {
        Map<String, String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing
                )
            );

        ApiErrorResponse response = ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation failed")
            .message("Invalid request data")
            .fieldErrors(errors)
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    // Other runtime errors
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
        RuntimeException e
    ) {
        ApiErrorResponse response = ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad request")
            .message(e.getMessage())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

}
