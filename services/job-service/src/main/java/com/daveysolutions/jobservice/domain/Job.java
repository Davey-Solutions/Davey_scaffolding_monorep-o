package com.daveysolutions.jobservice.domain;

import com.daveysolutions.jobservice.api.CreateJobRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity representing a scaffolding job.
 *
 * <p>A job tracks the customer, site, scope of work, pricing, scheduling, and
 * completion/payment state for a single scaffolding engagement.
 *
 * <p>{@code createdAt} and {@code updatedAt} are managed exclusively by the
 * JPA lifecycle callbacks {@link #prePersist()} and {@link #preUpdate()} and
 * therefore have no public setters.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job {

    /** Unique identifier for the job, generated automatically on first persist. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Full name of the customer who commissioned the job. */
    @NotBlank
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    /** Address of the site where the scaffolding will be erected. */
    @NotBlank
    @Column(name = "site_address", nullable = false)
    private String siteAddress;

    /** Free-text description of the work to be carried out. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Quoted or agreed price in GBP. */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    /** Current lifecycle status of the job; defaults to {@link JobStatus#PENDING}. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status = JobStatus.PENDING;

    /** Whether payment has been received for this job; defaults to {@code false}. */
    @Column(name = "paid", nullable = false)
    private boolean paid = false;

    /** Optional date on which scaffolding work is scheduled to begin. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Optional date on which scaffolding work is scheduled to finish. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Timestamp at which this job record was first persisted.
     * Set automatically by {@link #prePersist()} and never updated thereafter.
     */
    @Setter(AccessLevel.NONE)
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the most recent update to this job record.
     * Managed automatically by {@link #prePersist()} and {@link #preUpdate()}.
     */
    @Setter(AccessLevel.NONE)
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Constructs a new {@code Job} pre-populated from the given creation request.
     *
     * @param request the validated creation request containing required fields
     */
    public Job(CreateJobRequest request) {
        this.customerName = request.customerName();
        this.siteAddress = request.siteAddress();
    }

    /**
     * Updates mutable core fields for this job.
     *
     * @param customerName updated customer name
     * @param siteAddress  updated site address
     */
    public void update(String customerName, String siteAddress) {
        this.customerName = customerName;
        this.siteAddress = siteAddress;
    }

    /**
     * Sets {@link #createdAt} and {@link #updatedAt} to the current instant
     * immediately before the entity is first written to the database.
     */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Refreshes {@link #updatedAt} to the current instant immediately before
     * any subsequent update is written to the database.
     */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
