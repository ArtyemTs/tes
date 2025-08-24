package com.tes.api.web;
        
        import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

        import java.util.HashMap;
import java.util.List;
import java.util.Map;

        @ControllerAdvice
public class GlobalExceptionHandler {

          @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
            List<Map<String, Object>> violations = ex.getBindingResult().getFieldErrors().stream()
                        .map(this::toViolation)
                        .toList();
        
                    Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.BAD_REQUEST.value());
            body.put("error", "Bad Request");
            body.put("message", "Validation failed");
            body.put("violations", violations);
        
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body);
          }

          private Map<String, Object> toViolation(FieldError fe) {
            Map<String, Object> v = new HashMap<>();
            v.put("field", fe.getField());
            v.put("rejectedValue", fe.getRejectedValue());
            v.put("message", fe.getDefaultMessage());
            return v;
          }
}