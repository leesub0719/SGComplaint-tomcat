package com.transit.SGComplaint.monitoring;

import com.transit.SGComplaint.SGComplaintApplication;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SGComplaintApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "spring.flyway.enabled=false",
                "management.health.db.enabled=false", "management.health.diskspace.enabled=false"})
@Import(MonitoringTests.ProbeConfig.class)
class MonitoringTests {
    static final AtomicBoolean down = new AtomicBoolean();
    @Value("${local.management.port}") int managementPort;
    @Value("${local.server.port}") int appPort;
    @Value("${management.server.address}") String address;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @AfterEach void reset() { down.set(false); }

    HttpResponse<String> get(int port, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void healthReturnsOnlyStatus() throws Exception {
        var response = get(managementPort, "/actuator/health");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertFalse(response.body().contains("components"));
        assertFalse(response.body().contains("private-detail"));
    }

    @Test void failedContributorReturns503WithoutDetails() throws Exception {
        down.set(true);
        var response = get(managementPort, "/actuator/health");
        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"DOWN\""));
        assertFalse(response.body().contains("components"));
        assertFalse(response.body().contains("private-detail"));
    }

    @Test void sensitiveEndpointsAreNotPublic() throws Exception {
        for (String path : new String[]{"/actuator/env", "/actuator/configprops", "/actuator/heapdump", "/actuator/metrics"}) {
            var response = get(managementPort, path);
            assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, path);
        }
    }

    @Test void mainPortDoesNotExposeManagementHealth() throws Exception {
        assertNotEquals(appPort, managementPort);
        assertEquals(404, get(appPort, "/actuator/health").statusCode());
    }

    @Test void managementBindsToLoopback() {
        assertEquals("127.0.0.1", address);
    }

    @Test void livenessIsSeparateFromDependencyHealth() throws Exception {
        down.set(true);
        assertEquals(200, get(managementPort, "/actuator/health/liveness").statusCode());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfig {
        @Bean HealthIndicator testDependency() {
            return () -> (down.get() ? Health.down() : Health.up()).withDetail("private-detail", "not-public").build();
        }
    }
}
