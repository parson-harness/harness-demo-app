package io.harness.demo.service;

import io.harness.demo.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChaosService Tests")
class ChaosServiceTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private MetricsService metricsService;

    private ChaosService chaosService;

    @BeforeEach
    void setUp() {
        chaosService = new ChaosService(appConfig, metricsService);
    }

    @Nested
    @DisplayName("Dynamic Chaos Control Tests")
    class DynamicChaosTests {

        @Test
        @DisplayName("Should enable chaos with specified parameters")
        void enableChaos_shouldSetParameters() {
            chaosService.enableChaos(100, 0.5, 60);

            assertTrue(chaosService.isChaosActive());
            assertEquals(100, chaosService.getCurrentLatency());
            assertEquals(0.5, chaosService.getCurrentErrorRate(), 0.001);
            assertTrue(chaosService.getRemainingSeconds() > 0);
            verify(metricsService).incrementChaosEvents("cv_enabled");
        }

        @Test
        @DisplayName("Should disable chaos and reset all parameters")
        void disableChaos_shouldResetParameters() {
            chaosService.enableChaos(100, 0.5, 60);
            chaosService.disableChaos();

            assertFalse(chaosService.isChaosActive());
            assertEquals(0, chaosService.getCurrentLatency());
            assertEquals(0.0, chaosService.getCurrentErrorRate(), 0.001);
            assertEquals(0, chaosService.getRemainingSeconds());
            verify(metricsService).incrementChaosEvents("cv_disabled");
        }

        @Test
        @DisplayName("Should return false when chaos is not enabled")
        void isChaosActive_shouldReturnFalseWhenNotEnabled() {
            assertFalse(chaosService.isChaosActive());
        }

        @Test
        @DisplayName("Should return zero latency when chaos is not active")
        void getCurrentLatency_shouldReturnZeroWhenNotActive() {
            assertEquals(0, chaosService.getCurrentLatency());
        }

        @Test
        @DisplayName("Should return zero error rate when chaos is not active")
        void getCurrentErrorRate_shouldReturnZeroWhenNotActive() {
            assertEquals(0.0, chaosService.getCurrentErrorRate(), 0.001);
        }

        @Test
        @DisplayName("Should return zero remaining seconds when chaos is not active")
        void getRemainingSeconds_shouldReturnZeroWhenNotActive() {
            assertEquals(0, chaosService.getRemainingSeconds());
        }
    }

    @Nested
    @DisplayName("Config-based Chaos Tests")
    class ConfigBasedChaosTests {

        @Test
        @DisplayName("Should not inject chaos when disabled in config")
        void maybeInjectChaos_shouldNotInjectWhenDisabled() {
            when(appConfig.isChaosEnabled()).thenReturn(false);

            assertDoesNotThrow(() -> chaosService.maybeInjectChaos());
            verify(metricsService, never()).incrementChaosEvents(anyString());
        }

        @Test
        @DisplayName("Should inject latency when configured")
        void maybeInjectChaos_shouldInjectLatencyWhenConfigured() {
            when(appConfig.isChaosEnabled()).thenReturn(true);
            when(appConfig.getChaosLatencyMs()).thenReturn(10);
            when(appConfig.getChaosErrorRate()).thenReturn(0.0);

            long startTime = System.currentTimeMillis();
            chaosService.maybeInjectChaos();
            long elapsed = System.currentTimeMillis() - startTime;

            assertTrue(elapsed >= 10);
            verify(metricsService).incrementChaosEvents("latency");
        }

        @Test
        @DisplayName("Should not inject latency when set to zero")
        void maybeInjectChaos_shouldNotInjectLatencyWhenZero() {
            when(appConfig.isChaosEnabled()).thenReturn(true);
            when(appConfig.getChaosLatencyMs()).thenReturn(0);
            when(appConfig.getChaosErrorRate()).thenReturn(0.0);

            chaosService.maybeInjectChaos();
            verify(metricsService, never()).incrementChaosEvents("latency");
        }
    }

    @Nested
    @DisplayName("Manual Chaos Injection Tests")
    class ManualChaosTests {

        @Test
        @DisplayName("Should inject specified latency manually")
        void injectLatency_shouldSleepForSpecifiedTime() {
            long startTime = System.currentTimeMillis();
            chaosService.injectLatency(50);
            long elapsed = System.currentTimeMillis() - startTime;

            assertTrue(elapsed >= 50);
            verify(metricsService).incrementChaosEvents("latency_manual");
        }

        @Test
        @DisplayName("Should throw exception when injecting error manually")
        void injectError_shouldThrowException() {
            assertThrows(ResponseStatusException.class, () -> chaosService.injectError());
            verify(metricsService).incrementChaosEvents("error_manual");
            verify(metricsService).incrementErrors("chaos_manual");
        }
    }

    @Nested
    @DisplayName("Chaos Expiration Tests")
    class ChaosExpirationTests {

        @Test
        @DisplayName("Should auto-disable chaos after expiration")
        void isChaosActive_shouldAutoDisableAfterExpiration() throws InterruptedException {
            // Enable chaos for 1 second
            chaosService.enableChaos(0, 0.0, 1);
            assertTrue(chaosService.isChaosActive());

            // Wait for expiration
            Thread.sleep(1100);

            assertFalse(chaosService.isChaosActive());
        }

        @Test
        @DisplayName("Should remain active before expiration")
        void isChaosActive_shouldRemainActiveBeforeExpiration() {
            chaosService.enableChaos(0, 0.0, 60);
            assertTrue(chaosService.isChaosActive());
            assertTrue(chaosService.getRemainingSeconds() > 50);
        }
    }
}
