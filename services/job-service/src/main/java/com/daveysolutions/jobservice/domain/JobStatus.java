package com.daveysolutions.jobservice.domain;

/**
 * Lifecycle status of a {@link Job}.
 */
public enum JobStatus {
    /** The job has been created but work has not yet started. */
    PENDING,
    /** Scaffolding work is currently in progress on site. */
    IN_PROGRESS,
    /** All scaffolding work has been completed and the site cleared. */
    COMPLETED
}
