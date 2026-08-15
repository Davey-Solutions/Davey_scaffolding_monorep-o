package com.daveysolutions.jobservice.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying Bean Validation constraints on {@link UpdateJobRequest}.
 */
class UpdateJobRequestValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    /** Initialises a standard Bean Validation validator factory and validator. */
    @BeforeEach
    void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /** Closes the validator factory to release any held resources. */
    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void validRequest_noViolations() {
        UpdateJobRequest request = new UpdateJobRequest("ACME Ltd", "1 High Street");
        Set<ConstraintViolation<UpdateJobRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void nullCustomerName_violation() {
        UpdateJobRequest request = new UpdateJobRequest(null, "1 High Street");
        Set<ConstraintViolation<UpdateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerName"));
    }

    @Test
    void blankSiteAddress_violation() {
        UpdateJobRequest request = new UpdateJobRequest("ACME Ltd", " ");
        Set<ConstraintViolation<UpdateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("siteAddress"));
    }
}
