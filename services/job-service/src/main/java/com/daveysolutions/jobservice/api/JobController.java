package com.daveysolutions.jobservice.api;

import com.daveysolutions.jobservice.domain.Job;
import com.daveysolutions.jobservice.domain.JobRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

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
     * Returns every persisted job in insertion order defined by the repository.
     *
     * @return the complete list of persisted jobs
     */
    @GetMapping
    public List<Job> listJobs() {
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
}
