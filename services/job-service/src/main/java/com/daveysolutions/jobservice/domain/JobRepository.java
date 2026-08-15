package com.daveysolutions.jobservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Returns jobs filtered by optional status and paid values.
     *
     * @param status optional job lifecycle status filter
     * @param paid   optional payment state filter
     * @return jobs matching all supplied filters
     */
    @Query("""
            select j
            from Job j
            where (:status is null or j.status = :status)
              and (:paid is null or j.paid = :paid)
            """)
    List<Job> findAllByFilters(@Param("status") JobStatus status, @Param("paid") Boolean paid);
}
