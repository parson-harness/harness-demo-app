package io.harness.demo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppConfig Tests")
class AppConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have default app name")
        void shouldHaveDefaultAppName() {
            assertEquals("My Application", appConfig.getAppName());
        }

        @Test
        @DisplayName("Should have default version")
        void shouldHaveDefaultVersion() {
            assertEquals("1.0.0", appConfig.getVersion());
        }

        @Test
        @DisplayName("Should have default environment")
        void shouldHaveDefaultEnvironment() {
            assertEquals("development", appConfig.getEnvironment());
        }

        @Test
        @DisplayName("Should have default deployment target")
        void shouldHaveDefaultDeploymentTarget() {
            assertEquals("kubernetes", appConfig.getDeploymentTarget());
        }

        @Test
        @DisplayName("Should have default customer name")
        void shouldHaveDefaultCustomerName() {
            assertEquals("Harness Customer", appConfig.getCustomerName());
        }

        @Test
        @DisplayName("Should have chaos enabled by default")
        void shouldHaveChaosEnabledByDefault() {
            assertTrue(appConfig.isChaosEnabled());
        }

        @Test
        @DisplayName("Should have zero chaos latency by default")
        void shouldHaveZeroChaosLatencyByDefault() {
            assertEquals(0, appConfig.getChaosLatencyMs());
        }

        @Test
        @DisplayName("Should have zero chaos error rate by default")
        void shouldHaveZeroChaosErrorRateByDefault() {
            assertEquals(0.0, appConfig.getChaosErrorRate(), 0.001);
        }
    }

    @Nested
    @DisplayName("Effective Variant Tests")
    class EffectiveVariantTests {

        @Test
        @DisplayName("Should return deployment variant when set")
        void getEffectiveVariant_shouldReturnDeploymentVariantWhenSet() {
            appConfig.setDeploymentVariant("blue");
            assertEquals("blue", appConfig.getEffectiveVariant());
        }

        @Test
        @DisplayName("Should return deployment track when variant is empty")
        void getEffectiveVariant_shouldReturnTrackWhenVariantEmpty() {
            appConfig.setDeploymentVariant("");
            appConfig.setDeploymentTrack("canary");
            assertEquals("canary", appConfig.getEffectiveVariant());
        }

        @Test
        @DisplayName("Should return stable when both are empty")
        void getEffectiveVariant_shouldReturnStableWhenBothEmpty() {
            appConfig.setDeploymentVariant("");
            appConfig.setDeploymentTrack("");
            assertEquals("stable", appConfig.getEffectiveVariant());
        }

        @Test
        @DisplayName("Should prefer variant over track")
        void getEffectiveVariant_shouldPreferVariantOverTrack() {
            appConfig.setDeploymentVariant("green");
            appConfig.setDeploymentTrack("canary");
            assertEquals("green", appConfig.getEffectiveVariant());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty variant")
        void getEffectiveVariant_shouldHandleNullAndEmptyVariant(String variant) {
            appConfig.setDeploymentVariant(variant);
            appConfig.setDeploymentTrack("stable");
            assertEquals("stable", appConfig.getEffectiveVariant());
        }

        @Test
        @DisplayName("Should infer canary for ASG instances when deployment track env is empty")
        void getEffectiveVariant_shouldInferCanaryForAsgWhenTrackEmpty() {
            TestableAppConfig testableAppConfig = new TestableAppConfig("harness-demo-toddasg-asg__Canary");
            testableAppConfig.setDeploymentTarget("asg");
            testableAppConfig.setDeploymentVariant("");
            testableAppConfig.setDeploymentTrack("");

            assertEquals("canary", testableAppConfig.getDeploymentTrack());
            assertEquals("canary", testableAppConfig.getEffectiveVariant());
        }

        @Test
        @DisplayName("Should infer stable for non-canary ASG instances when deployment track env is empty")
        void getEffectiveVariant_shouldInferStableForAsgWhenTrackEmpty() {
            TestableAppConfig testableAppConfig = new TestableAppConfig("harness-demo-toddasg-asg");
            testableAppConfig.setDeploymentTarget("asg");
            testableAppConfig.setDeploymentVariant("");
            testableAppConfig.setDeploymentTrack("");

            assertEquals("stable", testableAppConfig.getDeploymentTrack());
            assertEquals("stable", testableAppConfig.getEffectiveVariant());
        }

        @Test
        @DisplayName("Should not infer ASG track when deployment target is not ASG")
        void getEffectiveVariant_shouldNotInferAsgTrackForNonAsgTarget() {
            TestableAppConfig testableAppConfig = new TestableAppConfig("harness-demo-toddasg-asg__Canary");
            testableAppConfig.setDeploymentTarget("kubernetes");
            testableAppConfig.setDeploymentVariant("");
            testableAppConfig.setDeploymentTrack("");

            assertEquals("", testableAppConfig.getDeploymentTrack());
            assertEquals("stable", testableAppConfig.getEffectiveVariant());
        }
    }

    @Nested
    @DisplayName("Deployment Strategy Tests")
    class DeploymentStrategyTests {

        @Test
        @DisplayName("Should infer blue-green when deployment variant is blue")
        void getEffectiveDeploymentStrategy_shouldInferBlueGreenWhenVariantIsBlue() {
            appConfig.setDeploymentStrategy("");
            appConfig.setDeploymentVariant("blue");
            appConfig.setDeploymentTrack("");

            assertEquals("blue-green", appConfig.getEffectiveDeploymentStrategy());
            assertEquals("Blue Green", appConfig.getDisplayDeploymentStrategy());
        }

        @Test
        @DisplayName("Should infer canary when deployment track is stable")
        void getEffectiveDeploymentStrategy_shouldInferCanaryWhenTrackIsStable() {
            appConfig.setDeploymentStrategy("");
            appConfig.setDeploymentVariant("");
            appConfig.setDeploymentTrack("stable");

            assertEquals("canary", appConfig.getEffectiveDeploymentStrategy());
            assertEquals("Canary", appConfig.getDisplayDeploymentStrategy());
        }

        @Test
        @DisplayName("Should prefer explicit deployment strategy when provided")
        void getEffectiveDeploymentStrategy_shouldPreferExplicitStrategyWhenProvided() {
            appConfig.setDeploymentStrategy("canary");
            appConfig.setDeploymentVariant("blue");
            appConfig.setDeploymentTrack("");

            assertEquals("canary", appConfig.getEffectiveDeploymentStrategy());
            assertEquals("Canary", appConfig.getDisplayDeploymentStrategy());
        }
    }

    @Nested
    @DisplayName("Deployment Target Display Tests")
    class DeploymentTargetDisplayTests {

        @Test
        @DisplayName("Should return Amazon EKS for k8s deployment target alias")
        void getDisplayDeploymentTarget_shouldReturnEksForK8sAlias() {
            appConfig.setDeploymentTarget("k8s");

            assertEquals("Amazon EKS", appConfig.getDisplayDeploymentTarget());
        }

        @Test
        @DisplayName("Should trim k8s deployment target alias before mapping")
        void getDisplayDeploymentTarget_shouldTrimK8sAliasBeforeMapping() {
            appConfig.setDeploymentTarget("  k8s  ");

            assertEquals("Amazon EKS", appConfig.getDisplayDeploymentTarget());
        }
    }

    @Nested
    @DisplayName("Variant Color Tests")
    class VariantColorTests {

        @ParameterizedTest
        @CsvSource({
            "blue, #3B82F6",
            "BLUE, #3B82F6",
            "Blue, #3B82F6",
            "green, #22C55E",
            "GREEN, #22C55E",
            "Green, #22C55E",
            "canary, #EAB308",
            "CANARY, #EAB308",
            "Canary, #EAB308",
            "stable, #8B5CF6",
            "STABLE, #8B5CF6",
            "Stable, #8B5CF6"
        })
        @DisplayName("Should return correct color for variant")
        void getVariantColor_shouldReturnCorrectColor(String variant, String expectedColor) {
            appConfig.setDeploymentVariant(variant);
            assertEquals(expectedColor, appConfig.getVariantColor());
        }

        @Test
        @DisplayName("Should return purple for unknown variant")
        void getVariantColor_shouldReturnPurpleForUnknown() {
            appConfig.setDeploymentVariant("unknown");
            assertEquals("#8B5CF6", appConfig.getVariantColor());
        }

        @Test
        @DisplayName("Should return purple when variant is empty")
        void getVariantColor_shouldReturnPurpleWhenEmpty() {
            appConfig.setDeploymentVariant("");
            appConfig.setDeploymentTrack("");
            assertEquals("#8B5CF6", appConfig.getVariantColor());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should set app name")
        void shouldSetAppName() {
            appConfig.setAppName("Custom App");
            assertEquals("Custom App", appConfig.getAppName());
        }

        @Test
        @DisplayName("Should set version")
        void shouldSetVersion() {
            appConfig.setVersion("2.0.0");
            assertEquals("2.0.0", appConfig.getVersion());
        }

        @Test
        @DisplayName("Should set environment")
        void shouldSetEnvironment() {
            appConfig.setEnvironment("production");
            assertEquals("production", appConfig.getEnvironment());
        }

        @Test
        @DisplayName("Should set chaos latency")
        void shouldSetChaosLatency() {
            appConfig.setChaosLatencyMs(500);
            assertEquals(500, appConfig.getChaosLatencyMs());
        }

        @Test
        @DisplayName("Should set chaos error rate")
        void shouldSetChaosErrorRate() {
            appConfig.setChaosErrorRate(0.25);
            assertEquals(0.25, appConfig.getChaosErrorRate(), 0.001);
        }

        @Test
        @DisplayName("Should set chaos enabled")
        void shouldSetChaosEnabled() {
            appConfig.setChaosEnabled(false);
            assertFalse(appConfig.isChaosEnabled());
        }

        @ParameterizedTest
        @ValueSource(strings = {"us-west-2", "eu-west-1", "ap-southeast-1"})
        @DisplayName("Should set region")
        void shouldSetRegion(String region) {
            appConfig.setRegion(region);
            assertEquals(region, appConfig.getRegion());
        }

        @Test
        @DisplayName("Should set namespace")
        void shouldSetNamespace() {
            appConfig.setNamespace("production");
            assertEquals("production", appConfig.getNamespace());
        }

        @Test
        @DisplayName("Should set pod name")
        void shouldSetPodName() {
            appConfig.setPodName("demo-app-abc123");
            assertEquals("demo-app-abc123", appConfig.getPodName());
        }

        @Test
        @DisplayName("Should set customer logo")
        void shouldSetCustomerLogo() {
            appConfig.setCustomerLogo("https://example.com/logo.png");
            assertEquals("https://example.com/logo.png", appConfig.getCustomerLogo());
        }
    }

    private static final class TestableAppConfig extends AppConfig {
        private final String autoScalingGroupName;

        private TestableAppConfig(String autoScalingGroupName) {
            this.autoScalingGroupName = autoScalingGroupName;
        }

        @Override
        protected String resolveAutoScalingGroupName() {
            return autoScalingGroupName;
        }
    }
}
