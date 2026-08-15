package com.daveysolutions.jobservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for the job-service REST API.
 *
 * <p>Converts Bean Validation failures into {@code 400 Bad Request} responses
 * using the RFC 7807 {@link ProblemDetail} format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link MethodArgumentNotValidException} thrown when a request
     * body fails Bean Validation and returns a structured {@code 400} response.
     *
     * @param ex the validation exception raised by Spring MVC
     * @return a {@link ProblemDetail} describing which fields are invalid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        return problem;
    }
}
