package com.daveysolutions.jobservice.api;

import com.daveysolutions.jobservice.domain.JobStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the {@code PUT /api/v1/jobs/{id}} endpoint.
 *
 * <p>Both {@code customerName} and {@code siteAddress} are required.
 *
 * @param customerName full name of the customer commissioning the job
 * @param siteAddress  address of the site where scaffolding will be erected
 * @param status       optional lifecycle status update
 * @param paid         optional payment state update
 */
public record UpdateJobRequest(

        /** Full name of the customer who commissioned the job. */
        @NotBlank(message = "customerName is required")
        String customerName,

        /** Address of the site where the scaffolding will be erected. */
        @NotBlank(message = "siteAddress is required")
        String siteAddress,

        /** Optional lifecycle status update. */
        JobStatus status,

        /** Optional payment state update. */
        Boolean paid
) {
    /**
     * Backward-compatible constructor using required fields only.
     *
     * @param customerName full name of the customer commissioning the job
     * @param siteAddress address of the site where the scaffolding will be erected
     */
    public UpdateJobRequest(String customerName, String siteAddress) {
        this(customerName, siteAddress, null, null);
    }
}
