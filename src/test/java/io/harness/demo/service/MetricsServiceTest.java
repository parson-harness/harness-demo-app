package io.harness.demo.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetricsService Tests")
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Nested
    @DisplayName("Page View Metrics Tests")
    class PageViewMetricsTests {

        @Test
        @DisplayName("Should increment page views for home page")
        void incrementPageViews_shouldIncrementForHomePage() {
            metricsService.incrementPageViews("home");
            metricsService.incrementPageViews("home");

            double count = meterRegistry.get("harness_demo_page_views_total")
                    .tag("page", "home")
                    .counter()
                    .count();

            assertEquals(2.0, count);
        }

        @Test
        @DisplayName("Should track different pages separately")
        void incrementPageViews_shouldTrackDifferentPagesSeparately() {
            metricsService.incrementPageViews("home");
            metricsService.incrementPageViews("services");
            metricsService.incrementPageViews("deployment");

            assertEquals(1.0, meterRegistry.get("harness_demo_page_views_total")
                    .tag("page", "home").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_page_views_total")
                    .tag("page", "services").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_page_views_total")
                    .tag("page", "deployment").counter().count());
        }
    }

    @Nested
    @DisplayName("API Call Metrics Tests")
    class ApiCallMetricsTests {

        @Test
        @DisplayName("Should increment API calls for endpoint")
        void incrementApiCalls_shouldIncrementForEndpoint() {
            metricsService.incrementApiCalls("/api/info");
            metricsService.incrementApiCalls("/api/info");
            metricsService.incrementApiCalls("/api/info");

            double count = meterRegistry.get("harness_demo_api_calls_total")
                    .tag("endpoint", "/api/info")
                    .counter()
                    .count();

            assertEquals(3.0, count);
        }

        @Test
        @DisplayName("Should track different endpoints separately")
        void incrementApiCalls_shouldTrackDifferentEndpointsSeparately() {
            metricsService.incrementApiCalls("/api/info");
            metricsService.incrementApiCalls("/api/health");
            metricsService.incrementApiCalls("/api/version");

            assertEquals(1.0, meterRegistry.get("harness_demo_api_calls_total")
                    .tag("endpoint", "/api/info").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_api_calls_total")
                    .tag("endpoint", "/api/health").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_api_calls_total")
                    .tag("endpoint", "/api/version").counter().count());
        }
    }

    @Nested
    @DisplayName("Processing Time Metrics Tests")
    class ProcessingTimeMetricsTests {

        @Test
        @DisplayName("Should record processing time")
        void recordProcessingTime_shouldRecordDuration() {
            metricsService.recordProcessingTime(100);
            metricsService.recordProcessingTime(200);
            metricsService.recordProcessingTime(150);

            long count = meterRegistry.get("harness_demo_processing_duration")
                    .timer()
                    .count();

            assertEquals(3, count);
        }

        @Test
        @DisplayName("Should record zero processing time")
        void recordProcessingTime_shouldRecordZeroDuration() {
            metricsService.recordProcessingTime(0);

            long count = meterRegistry.get("harness_demo_processing_duration")
                    .timer()
                    .count();

            assertEquals(1, count);
        }
    }

    @Nested
    @DisplayName("Chaos Event Metrics Tests")
    class ChaosEventMetricsTests {

        @Test
        @DisplayName("Should increment chaos events by type")
        void incrementChaosEvents_shouldIncrementByType() {
            metricsService.incrementChaosEvents("latency");
            metricsService.incrementChaosEvents("latency");
            metricsService.incrementChaosEvents("error");

            assertEquals(2.0, meterRegistry.get("harness_demo_chaos_events_total")
                    .tag("type", "latency").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_chaos_events_total")
                    .tag("type", "error").counter().count());
        }

        @Test
        @DisplayName("Should track CV chaos events separately")
        void incrementChaosEvents_shouldTrackCvEventsSeparately() {
            metricsService.incrementChaosEvents("cv_enabled");
            metricsService.incrementChaosEvents("cv_latency");
            metricsService.incrementChaosEvents("cv_error");
            metricsService.incrementChaosEvents("cv_disabled");

            assertEquals(1.0, meterRegistry.get("harness_demo_chaos_events_total")
                    .tag("type", "cv_enabled").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_chaos_events_total")
                    .tag("type", "cv_latency").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_chaos_events_total")
                    .tag("type", "cv_error").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_chaos_events_total")
                    .tag("type", "cv_disabled").counter().count());
        }
    }

    @Nested
    @DisplayName("Error Metrics Tests")
    class ErrorMetricsTests {

        @Test
        @DisplayName("Should increment errors by type")
        void incrementErrors_shouldIncrementByType() {
            metricsService.incrementErrors("chaos_injected");
            metricsService.incrementErrors("chaos_injected");
            metricsService.incrementErrors("validation");

            assertEquals(2.0, meterRegistry.get("harness_demo_errors_total")
                    .tag("type", "chaos_injected").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_errors_total")
                    .tag("type", "validation").counter().count());
        }

        @Test
        @DisplayName("Should track different error types")
        void incrementErrors_shouldTrackDifferentTypes() {
            metricsService.incrementErrors("timeout");
            metricsService.incrementErrors("connection");
            metricsService.incrementErrors("internal");

            assertEquals(1.0, meterRegistry.get("harness_demo_errors_total")
                    .tag("type", "timeout").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_errors_total")
                    .tag("type", "connection").counter().count());
            assertEquals(1.0, meterRegistry.get("harness_demo_errors_total")
                    .tag("type", "internal").counter().count());
        }
    }
}
