package io.harness.demo.controller;

import io.harness.demo.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppConfig appConfig;

    @Test
    void getInfo_shouldReturnAppInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.version").value(appConfig.getVersion()))
                .andExpect(jsonPath("$.deploymentVariant").value(appConfig.getEffectiveVariant()));
    }

    @Test
    void getHealth_shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void getVersion_shouldReturnVersionInfo() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.variant").exists());
    }

    @Test
    void process_shouldProcessRequest() throws Exception {
        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"test\": \"data\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("processed"))
                .andExpect(jsonPath("$.inputReceived").value(true));
    }

    @Test
    void simulateLoad_shouldReturnDuration() throws Exception {
        mockMvc.perform(get("/api/simulate-load").param("durationMs", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedDurationMs").value(100))
                .andExpect(jsonPath("$.actualDurationMs").exists());
    }
}
