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
 * and missing-resource lookups into {@code 404 Not Found} responses, both
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

        return problemDetail(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    /**
     * Handles {@link JobNotFoundException} thrown when the requested job
     * does not exist and returns a {@code 404 Not Found} response.
     *
     * @param ex the exception raised by the controller
     * @return a {@link ProblemDetail} describing the missing resource
     */
    @ExceptionHandler(JobNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleJobNotFound(JobNotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Job not found", ex.getMessage());
    }

    /**
     * Builds a {@link ProblemDetail} with the given status, title, and detail.
     *
     * <p>All three values are passed to the {@link TitledProblemDetail} constructor
     * so that both the RFC 7807 {@code title} and {@code detail} fields are
     * populated at object-creation time without any post-construction setters.
     *
     * @param status the HTTP status
     * @param title  the RFC 7807 problem title
     * @param detail the RFC 7807 problem detail
     * @return a fully populated {@link ProblemDetail}
     */
    private static ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        return new TitledProblemDetail(status.value(), title, detail);
    }

    /**
     * A minimal {@link ProblemDetail} subclass that accepts the RFC 7807
     * {@code title} and {@code detail} values as constructor arguments,
     * avoiding any post-construction setter calls.
     */
    private static final class TitledProblemDetail extends ProblemDetail {

        /**
         * Creates a {@link TitledProblemDetail} with the given HTTP status code,
         * RFC 7807 title, and RFC 7807 detail.
         *
         * @param rawStatusCode the raw HTTP status code
         * @param title         the RFC 7807 problem title
         * @param detail        the RFC 7807 problem detail
         */
        private TitledProblemDetail(int rawStatusCode, String title, String detail) {
            super(rawStatusCode);
            setTitle(title);
            setDetail(detail);
        }
    }
}
