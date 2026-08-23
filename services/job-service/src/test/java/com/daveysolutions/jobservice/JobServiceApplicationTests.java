package com.daveysolutions.jobservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobServiceApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void metricsEndpointReportsRequestSignals() {
        testRestTemplate.getForEntity("http://localhost:" + port + "/actuator/health", Map.class);
        testRestTemplate.getForEntity("http://localhost:" + port + "/api/v1/jobs", String.class);

        final ResponseEntity<Map> response = testRestTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics/http.server.requests",
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("name", "http.server.requests");
        assertThat(extractTagValues(response.getBody(), "status")).contains("200", "401");
        assertThat(extractMeasurementStatistics(response.getBody())).contains("COUNT", "TOTAL_TIME", "MAX");
    }

    private List<String> extractTagValues(Map body, String tagName) {
        return (List<String>) ((List<Map<String, Object>>) body.get("availableTags")).stream()
                .filter(tag -> tagName.equals(tag.get("tag")))
                .findFirst()
                .orElseThrow()
                .get("values");
    }

    private List<String> extractMeasurementStatistics(Map body) {
        return ((List<Map<String, Object>>) body.get("measurements")).stream()
                .map(measurement -> String.valueOf(measurement.get("statistic")))
                .toList();
    }
}
