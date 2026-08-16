package com.daveysolutions.jobservice.api;

import java.util.UUID;

/**
 * Thrown when a requested job does not exist in the repository.
 */
public class JobNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception for the given job identifier.
     *
     * @param id the identifier of the missing job
     */
    public JobNotFoundException(UUID id) {
        super("Job not found: " + id);
    }
}
