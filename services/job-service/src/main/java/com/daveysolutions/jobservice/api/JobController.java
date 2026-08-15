package com.daveysolutions.jobservice.api;

import com.daveysolutions.jobservice.domain.Job;
import com.daveysolutions.jobservice.domain.JobRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * REST controller for job resources.
 *
 * <p>Exposes endpoints under {@code /api/v1/jobs}.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    /** Repository used to persist job entities. */
    private final JobRepository jobRepository;

    /**
     * Creates a new job from the supplied request body.
     *
     * <p>Returns {@code 201 Created} together with the persisted job
     * (including its generated {@code id} and audit timestamps).
     * Returns {@code 400 Bad Request} when {@code customerName} or
     * {@code siteAddress} is absent or blank.
     *
     * @param request the validated request body
     * @return the newly created {@link Job}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job createJob(@Valid @RequestBody CreateJobRequest request) {
        return jobRepository.save(new Job(request));
    }

    /**
     * Updates an existing job using the supplied request body.
     *
     * <p>Returns {@code 200 OK} together with the updated job.
     * Returns {@code 400 Bad Request} when {@code customerName} or
     * {@code siteAddress} is absent or blank.
     * Returns {@code 404 Not Found} when no job exists for {@code id}.
     *
     * @param id      unique identifier of the job to update
     * @param request the validated request body
     * @return the updated {@link Job}
     */
    @PutMapping("/{id}")
    public Job updateJob(@PathVariable UUID id, @Valid @RequestBody UpdateJobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        job.setCustomerName(request.customerName());
        job.setSiteAddress(request.siteAddress());
        return jobRepository.save(job);
    }
}
