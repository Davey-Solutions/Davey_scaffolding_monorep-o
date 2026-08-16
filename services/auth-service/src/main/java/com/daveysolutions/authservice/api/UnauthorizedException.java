package com.daveysolutions.authservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when authentication credentials are invalid or not found.
 *
 * <p>Mapped to HTTP 401 Unauthorized by the {@link ResponseStatus} annotation.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /** Creates an {@link UnauthorizedException} with no detail message. */
    public UnauthorizedException() {
        super();
    }
}
