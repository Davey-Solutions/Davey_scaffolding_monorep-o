package com.daveysolutions.jobservice.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the {@code POST /api/v1/jobs} endpoint.
 *
 * <p>Both {@code customerName} and {@code siteAddress} are required; all other
 * fields are optional and will be ignored at creation time.
 *
 * @param customerName full name of the customer commissioning the job
 * @param siteAddress  address of the site where scaffolding will be erected
 */
public record CreateJobRequest(

        /** Full name of the customer who commissioned the job. */
        @NotBlank(message = "customerName is required")
        String customerName,

        /** Address of the site where the scaffolding will be erected. */
        @NotBlank(message = "siteAddress is required")
        String siteAddress
) {}
