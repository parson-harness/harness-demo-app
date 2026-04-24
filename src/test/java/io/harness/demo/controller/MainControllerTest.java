package io.harness.demo.controller;

import io.harness.demo.config.AppConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MainController Tests")
class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppConfig appConfig;

    @Nested
    @DisplayName("Home Page Tests")
    class HomePageTests {

        @Test
        @DisplayName("Should render home page with default values")
        void home_shouldRenderWithDefaults() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("config"))
                    .andExpect(model().attributeExists("hostname"))
                    .andExpect(model().attributeExists("timestamp"))
                    .andExpect(model().attributeExists("javaVersion"));
        }

        @Test
        @DisplayName("Should use custom name from query parameter")
        void home_shouldUseCustomName() throws Exception {
            mockMvc.perform(get("/").param("name", "John Doe"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("userName", "John Doe"));
        }

        @Test
        @DisplayName("Should use custom company from query parameter")
        void home_shouldUseCustomCompany() throws Exception {
            mockMvc.perform(get("/").param("company", "Acme Corp"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("company", "Acme Corp"));
        }

        @Test
        @DisplayName("Should use both custom name and company")
        void home_shouldUseBothCustomValues() throws Exception {
            mockMvc.perform(get("/")
                            .param("name", "Jane Smith")
                            .param("company", "Tech Inc"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("userName", "Jane Smith"))
                    .andExpect(model().attribute("company", "Tech Inc"));
        }

        @Test
        @DisplayName("Should use default customer name when not provided")
        void home_shouldUseDefaultCustomerName() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("userName", appConfig.getCustomerName()));
        }

        @Test
        @DisplayName("Should use default company when not provided")
        void home_shouldUseDefaultCompany() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("company", "Harness"));
        }
    }

    @Nested
    @DisplayName("Services Page Tests")
    class ServicesPageTests {

        @Test
        @DisplayName("Should render services page")
        void services_shouldRenderPage() throws Exception {
            mockMvc.perform(get("/services"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("services"))
                    .andExpect(model().attributeExists("config"));
        }
    }

    @Nested
    @DisplayName("Deployment Page Tests")
    class DeploymentPageTests {

        @Test
        @DisplayName("Should render deployment page")
        void deployment_shouldRenderPage() throws Exception {
            mockMvc.perform(get("/deployment"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("deployment"))
                    .andExpect(model().attributeExists("config"))
                    .andExpect(model().attributeExists("hostname"));
        }
    }

    @Nested
    @DisplayName("Resilience Page Tests")
    class ResiliencePageTests {

        @Test
        @DisplayName("Should render resilience page")
        void resilience_shouldRenderPage() throws Exception {
            mockMvc.perform(get("/resilience"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("resilience"))
                    .andExpect(model().attributeExists("config"));
        }
    }
}
