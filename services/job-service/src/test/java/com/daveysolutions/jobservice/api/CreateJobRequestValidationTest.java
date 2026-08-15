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
 * Unit tests verifying Bean Validation constraints on {@link CreateJobRequest}.
 */
class CreateJobRequestValidationTest {

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
        CreateJobRequest request = new CreateJobRequest("ACME Ltd", "1 High Street");
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void nullCustomerName_violation() {
        CreateJobRequest request = new CreateJobRequest(null, "1 High Street");
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerName"));
    }

    @Test
    void blankCustomerName_violation() {
        CreateJobRequest request = new CreateJobRequest("  ", "1 High Street");
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerName"));
    }

    @Test
    void nullSiteAddress_violation() {
        CreateJobRequest request = new CreateJobRequest("ACME Ltd", null);
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("siteAddress"));
    }

    @Test
    void blankSiteAddress_violation() {
        CreateJobRequest request = new CreateJobRequest("ACME Ltd", "");
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("siteAddress"));
    }

    @Test
    void bothFieldsMissing_violationsForBothFields() {
        CreateJobRequest request = new CreateJobRequest(null, null);
        Set<ConstraintViolation<CreateJobRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerName"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("siteAddress"));
    }
}
