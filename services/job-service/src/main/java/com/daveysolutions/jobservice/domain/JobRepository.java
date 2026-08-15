package com.daveysolutions.jobservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for {@link Job} entities.
 *
 * <p>Provides standard CRUD operations and query-by-example support out of the
 * box via {@link JpaRepository}.
 */
public interface JobRepository extends JpaRepository<Job, UUID> {
}
