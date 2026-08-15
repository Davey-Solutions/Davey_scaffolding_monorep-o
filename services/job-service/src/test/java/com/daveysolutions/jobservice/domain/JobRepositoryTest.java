package com.daveysolutions.jobservice.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JobRepository jobRepository;

    @Test
    void migrationCreatesJobsTable() {
        assertThat(jobRepository.count()).isZero();
    }

    @Test
    void saveAndFindJob() {
        Job job = new Job();
        job.setCustomerName("Test Customer");
        job.setSiteAddress("123 Scaffolding St");
        job.setDescription("Erect scaffolding on front elevation");
        job.setPrice(new BigDecimal("1500.00"));
        job.setStatus(JobStatus.PENDING);
        job.setPaid(false);
        job.setStartDate(LocalDate.of(2026, 9, 1));
        job.setEndDate(LocalDate.of(2026, 9, 10));

        Job saved = jobRepository.save(job);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Job found = jobRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCustomerName()).isEqualTo("Test Customer");
        assertThat(found.getSiteAddress()).isEqualTo("123 Scaffolding St");
        assertThat(found.getDescription()).isEqualTo("Erect scaffolding on front elevation");
        assertThat(found.getPrice()).isEqualByComparingTo("1500.00");
        assertThat(found.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(found.isPaid()).isFalse();
        assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(found.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    void statusDefaultsPending() {
        Job job = new Job();
        job.setCustomerName("Another Customer");
        job.setSiteAddress("456 Builder Ave");

        Job saved = jobRepository.save(job);

        assertThat(saved.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(saved.isPaid()).isFalse();
    }
}
