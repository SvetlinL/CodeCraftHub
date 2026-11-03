package com.codecrafthub.controller;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    // Return useful validation details without exposing internal file paths.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Required fields are missing or invalid.");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField().equals("targetDate") ? "target_date" : error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail invalidJson(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Invalid JSON. target_date must be a real date in YYYY-MM-DD format; status must be exactly Not Started, In Progress, or Completed.");
    }
    @ExceptionHandler(IOException.class)
    public ProblemDetail storageFailure(IOException ex) {
        log.error("Unable to access course JSON file", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read or save course data. Check the server log and course file.");
    }
}
