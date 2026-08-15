package com.daveysolutions.jobservice.api;

import com.daveysolutions.jobservice.domain.Job;
import com.daveysolutions.jobservice.domain.JobRepository;
import com.daveysolutions.jobservice.domain.JobStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
     * Returns persisted jobs, optionally filtered by status and payment state.
     *
     * @param status optional status filter
     * @param paid   optional paid filter
     * @return the matching list of persisted jobs
     */
    @GetMapping
    public List<Job> listJobs(@RequestParam(value = "status", required = false) JobStatus status,
                              @RequestParam(value = "paid", required = false) Boolean paid) {
        if (status != null && paid != null) {
            return jobRepository.findByStatusAndPaid(status, paid);
        }
        if (status != null) {
            return jobRepository.findByStatus(status);
        }
        if (paid != null) {
            return jobRepository.findByPaid(paid);
        }
        return jobRepository.findAll();
    }

    /**
     * Returns the job identified by the supplied id.
     *
     * @param id the unique job identifier
     * @return the matching {@link Job}
     * @throws ResponseStatusException when no job exists for the supplied id
     */
    @GetMapping("/{id}")
    public Job getJob(@PathVariable("id") UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + id));
    }

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
    public Job updateJob(@PathVariable("id") UUID id, @Valid @RequestBody UpdateJobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        job.update(request);
        return jobRepository.save(job);
    }
}
