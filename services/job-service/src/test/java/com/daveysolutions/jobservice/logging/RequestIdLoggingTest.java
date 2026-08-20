package com.daveysolutions.jobservice.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that job-service logs and returns propagated request IDs.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class RequestIdLoggingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listJobsLogsProvidedRequestId(CapturedOutput output) throws Exception {
        String requestId = "job-test-request-id";

        mockMvc.perform(get("/api/v1/jobs")
                        .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, requestId));

        assertThat(output.getOut()).contains("\"requestId\":\"" + requestId + "\"");
    }
}
