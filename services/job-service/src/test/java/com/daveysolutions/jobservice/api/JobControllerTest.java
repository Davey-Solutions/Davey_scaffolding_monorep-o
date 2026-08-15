package com.daveysolutions.jobservice.api;

import com.daveysolutions.jobservice.domain.Job;
import com.daveysolutions.jobservice.domain.JobRepository;
import com.daveysolutions.jobservice.domain.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link WebMvcTest} slice tests for {@link JobController}.
 *
 * <p>Uses a mocked {@link JobRepository} so no database is required.
 */
@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobRepository jobRepository;

    /** Builds a minimal persisted {@link Job} stub for mock returns. */
    private Job buildSavedJob(String customerName, String siteAddress) {
        Job job = new Job(new CreateJobRequest(customerName, siteAddress));
        // simulate prePersist side-effects
        try {
            var idField = Job.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(job, UUID.randomUUID());
            var caField = Job.class.getDeclaredField("createdAt");
            caField.setAccessible(true);
            caField.set(job, Instant.now());
            var uaField = Job.class.getDeclaredField("updatedAt");
            uaField.setAccessible(true);
            uaField.set(job, Instant.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return job;
    }

    @Test
    void listJobs_returnsAllJobs() throws Exception {
        Job firstJob = buildSavedJob("ACME Ltd", "1 High Street");
        Job secondJob = buildSavedJob("Beta Ltd", "2 Low Street");
        when(jobRepository.findAllByFilters(null, null)).thenReturn(List.of(firstJob, secondJob));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerName").value("ACME Ltd"))
                .andExpect(jsonPath("$[0].siteAddress").value("1 High Street"))
                .andExpect(jsonPath("$[1].customerName").value("Beta Ltd"))
                .andExpect(jsonPath("$[1].siteAddress").value("2 Low Street"));

        verify(jobRepository).findAllByFilters(null, null);
    }

    @Test
    void listJobs_filterByStatus_returnsMatchingJobs() throws Exception {
        Job job = buildSavedJob("ACME Ltd", "1 High Street");
        when(jobRepository.findAllByFilters(JobStatus.COMPLETED, null)).thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/jobs").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("ACME Ltd"));

        verify(jobRepository).findAllByFilters(JobStatus.COMPLETED, null);
    }

    @Test
    void listJobs_filterByPaid_returnsMatchingJobs() throws Exception {
        Job job = buildSavedJob("Paid Co", "3 Main Road");
        when(jobRepository.findAllByFilters(null, true)).thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/jobs").param("paid", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Paid Co"));

        verify(jobRepository).findAllByFilters(null, true);
    }

    @Test
    void listJobs_filterByStatusAndPaid_returnsMatchingJobs() throws Exception {
        Job job = buildSavedJob("Done & Paid", "4 Side Lane");
        when(jobRepository.findAllByFilters(JobStatus.COMPLETED, true)).thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/jobs")
                        .param("status", "COMPLETED")
                        .param("paid", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Done & Paid"));

        verify(jobRepository).findAllByFilters(JobStatus.COMPLETED, true);
    }

    @Test
    void listJobs_filterByInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").param("status", "NOT_A_REAL_STATUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobRepository);
    }

    @Test
    void getJob_existingId_returnsJob() throws Exception {
        Job saved = buildSavedJob("ACME Ltd", "1 High Street");
        when(jobRepository.findById(saved.getId())).thenReturn(Optional.of(saved));

        mockMvc.perform(get("/api/v1/jobs/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.customerName").value("ACME Ltd"))
                .andExpect(jsonPath("$.siteAddress").value("1 High Street"));
    }

    @Test
    void getJob_missingId_returns404() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(jobRepository.findById(missingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/jobs/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createJob_validRequest_returns201AndBody() throws Exception {
        Job saved = buildSavedJob("ACME Ltd", "1 High Street");
        when(jobRepository.save(any(Job.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(
                new CreateJobRequest("ACME Ltd", "1 High Street"));

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("ACME Ltd"))
                .andExpect(jsonPath("$.siteAddress").value("1 High Street"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createJob_setsFieldsBeforeSave() throws Exception {
        Job saved = buildSavedJob("ACME Ltd", "1 High Street");
        when(jobRepository.save(any(Job.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(
                new CreateJobRequest("ACME Ltd", "1 High Street"));

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerName()).isEqualTo("ACME Ltd");
        assertThat(captor.getValue().getSiteAddress()).isEqualTo("1 High Street");
    }

    @Test
    void createJob_missingCustomerName_returns400() throws Exception {
        String body = "{\"siteAddress\":\"1 High Street\"}";

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJob_blankCustomerName_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateJobRequest("   ", "1 High Street"));

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJob_missingSiteAddress_returns400() throws Exception {
        String body = "{\"customerName\":\"ACME Ltd\"}";

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJob_bothFieldsMissing_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateJob_validRequest_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        Job existing = buildSavedJob("Old Customer", "Old Address");
        Job updated = buildSavedJob("New Customer", "New Address");
        try {
            var idField = Job.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existing, id);
            idField.set(updated, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(jobRepository.findById(id)).thenReturn(Optional.of(existing));
        when(jobRepository.save(any(Job.class))).thenReturn(updated);

        String body = objectMapper.writeValueAsString(new UpdateJobRequest("New Customer", "New Address"));

        mockMvc.perform(put("/api/v1/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.customerName").value("New Customer"))
                .andExpect(jsonPath("$.siteAddress").value("New Address"));

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
        assertThat(captor.getValue().getCustomerName()).isEqualTo("New Customer");
        assertThat(captor.getValue().getSiteAddress()).isEqualTo("New Address");
    }

    @Test
    void updateJob_missingCustomerName_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"siteAddress\":\"1 High Street\"}";

        mockMvc.perform(put("/api/v1/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobRepository);
    }

    @Test
    void updateJob_blankSiteAddress_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new UpdateJobRequest("ACME Ltd", "   "));

        mockMvc.perform(put("/api/v1/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobRepository);
    }

    @Test
    void updateJob_nonExistentId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        String body = objectMapper.writeValueAsString(new UpdateJobRequest("ACME Ltd", "1 High Street"));

        mockMvc.perform(put("/api/v1/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        verify(jobRepository, never()).save(any(Job.class));
    }
}
