package com.daveysolutions.jobservice.api;

import com.daveysolutions.jobservice.domain.JobStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the {@code POST /api/v1/jobs} endpoint.
 *
 * <p>Both {@code customerName} and {@code siteAddress} are required.
 * Optional fields allow explicitly setting initial status and payment state.
 *
 * @param customerName full name of the customer commissioning the job
 * @param siteAddress  address of the site where scaffolding will be erected
 * @param status       optional initial lifecycle status
 * @param paid         optional initial payment state
 */
public record CreateJobRequest(

        /** Full name of the customer who commissioned the job. */
        @NotBlank(message = "customerName is required")
        String customerName,

        /** Address of the site where the scaffolding will be erected. */
        @NotBlank(message = "siteAddress is required")
        String siteAddress,

        /** Optional initial lifecycle status. */
        JobStatus status,

        /** Optional initial payment state. */
        Boolean paid
) {
    /**
     * Backward-compatible constructor using required fields only.
     *
     * @param customerName full name of the customer commissioning the job
     * @param siteAddress address of the site where the scaffolding will be erected
     */
    public CreateJobRequest(String customerName, String siteAddress) {
        this(customerName, siteAddress, null, null);
    }
}
