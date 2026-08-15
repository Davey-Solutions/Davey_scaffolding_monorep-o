package com.daveysolutions.jobservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link Job} entities.
 *
 * <p>Provides standard CRUD operations and query-by-example support out of the
 * box via {@link JpaRepository}.
 */
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Returns jobs matching the supplied status.
     *
     * @param status job lifecycle status
     * @return jobs with the supplied status
     */
    List<Job> findByStatus(JobStatus status);

    /**
     * Returns jobs matching the supplied paid flag.
     *
     * @param paid payment state
     * @return jobs with the supplied payment state
     */
    List<Job> findByPaid(boolean paid);

    /**
     * Returns jobs matching both status and paid flag.
     *
     * @param status job lifecycle status
     * @param paid   payment state
     * @return jobs matching both filters
     */
    List<Job> findByStatusAndPaid(JobStatus status, boolean paid);
}
