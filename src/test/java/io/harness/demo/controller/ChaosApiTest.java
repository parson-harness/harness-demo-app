package io.harness.demo.controller;

import io.harness.demo.config.AppConfig;
import io.harness.demo.service.ChaosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Chaos API Tests")
class ChaosApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ChaosService chaosService;

    @Nested
    @DisplayName("Chaos Status Tests")
    class ChaosStatusTests {

        @Test
        @DisplayName("Should return chaos status when inactive")
        void getChaosStatus_shouldReturnInactiveStatus() throws Exception {
            chaosService.disableChaos();

            mockMvc.perform(get("/api/chaos/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.enabled").value(false))
                    .andExpect(jsonPath("$.latencyMs").value(0))
                    .andExpect(jsonPath("$.errorRate").value(0.0));
        }

        @Test
        @DisplayName("Should return chaos status when active")
        void getChaosStatus_shouldReturnActiveStatus() throws Exception {
            chaosService.enableChaos(100, 0.25, 60);

            try {
                mockMvc.perform(get("/api/chaos/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.enabled").value(true))
                        .andExpect(jsonPath("$.latencyMs").value(100))
                        .andExpect(jsonPath("$.errorRate").value(0.25));
            } finally {
                chaosService.disableChaos();
            }
        }

        @Test
        @DisplayName("Should return remaining seconds when active")
        void getChaosStatus_shouldReturnRemainingSeconds() throws Exception {
            chaosService.enableChaos(0, 0.0, 60);

            try {
                mockMvc.perform(get("/api/chaos/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.remainingSeconds").exists());
            } finally {
                chaosService.disableChaos();
            }
        }
    }

    @Nested
    @DisplayName("Enable Chaos Tests")
    class EnableChaosTests {

        @Test
        @DisplayName("Should enable chaos with default parameters")
        void enableChaos_shouldEnableWithDefaults() throws Exception {
            try {
                mockMvc.perform(post("/api/chaos/enable"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("chaos_enabled"))
                        .andExpect(jsonPath("$.latencyMs").exists())
                        .andExpect(jsonPath("$.errorRate").exists());
            } finally {
                chaosService.disableChaos();
            }
        }

        @Test
        @DisplayName("Should enable chaos with custom latency")
        void enableChaos_shouldEnableWithCustomLatency() throws Exception {
            try {
                mockMvc.perform(post("/api/chaos/enable")
                                .param("latencyMs", "200"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.latencyMs").value(200));
            } finally {
                chaosService.disableChaos();
            }
        }

        @Test
        @DisplayName("Should enable chaos with custom error rate")
        void enableChaos_shouldEnableWithCustomErrorRate() throws Exception {
            try {
                mockMvc.perform(post("/api/chaos/enable")
                                .param("errorRate", "0.5"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.errorRate").value(0.5));
            } finally {
                chaosService.disableChaos();
            }
        }

        @Test
        @DisplayName("Should enable chaos with custom duration")
        void enableChaos_shouldEnableWithCustomDuration() throws Exception {
            try {
                mockMvc.perform(post("/api/chaos/enable")
                                .param("durationSeconds", "120"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.durationSeconds").value(120));
            } finally {
                chaosService.disableChaos();
            }
        }

        @Test
        @DisplayName("Should enable chaos with all custom parameters")
        void enableChaos_shouldEnableWithAllCustomParams() throws Exception {
            try {
                mockMvc.perform(post("/api/chaos/enable")
                                .param("latencyMs", "300")
                                .param("errorRate", "0.4")
                                .param("durationSeconds", "90"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.latencyMs").value(300))
                        .andExpect(jsonPath("$.errorRate").value(0.4))
                        .andExpect(jsonPath("$.durationSeconds").value(90));
            } finally {
                chaosService.disableChaos();
            }
        }
    }

    @Nested
    @DisplayName("Disable Chaos Tests")
    class DisableChaosTests {

        @Test
        @DisplayName("Should disable chaos")
        void disableChaos_shouldDisable() throws Exception {
            chaosService.enableChaos(100, 0.5, 60);

            mockMvc.perform(post("/api/chaos/disable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("chaos_disabled"));

            mockMvc.perform(get("/api/chaos/status"))
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("Should be idempotent when already disabled")
        void disableChaos_shouldBeIdempotent() throws Exception {
            chaosService.disableChaos();

            mockMvc.perform(post("/api/chaos/disable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("chaos_disabled"));
        }
    }

    @Nested
    @DisplayName("Degrade Canary Tests")
    class DegradeCanaryTests {

        @Test
        @DisplayName("Should skip degradation for non-canary deployments")
        void degradeCanary_shouldSkipForNonCanary() throws Exception {
            appConfig.setDeploymentVariant("");
            appConfig.setDeploymentTrack("");

            mockMvc.perform(post("/api/chaos/degrade-canary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("skipped"))
                    .andExpect(jsonPath("$.variant").value("stable"))
                    .andExpect(jsonPath("$.message").value("Not a canary deployment - chaos not injected"));
        }

        @Test
        @DisplayName("Should degrade canary when deployment track is canary")
        void degradeCanary_shouldUseDeploymentTrack() throws Exception {
            appConfig.setDeploymentVariant("");
            appConfig.setDeploymentTrack("canary");

            try {
                mockMvc.perform(post("/api/chaos/degrade-canary"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("canary_degraded"))
                        .andExpect(jsonPath("$.variant").value("canary"));
            } finally {
                appConfig.setDeploymentTrack("");
                chaosService.disableChaos();
            }
        }
    }
}
