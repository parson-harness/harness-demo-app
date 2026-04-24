package io.harness.demo.controller;

import io.harness.demo.config.AppConfig;
import io.harness.demo.service.ChaosService;
import io.harness.demo.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final AppConfig appConfig;
    private final MetricsService metricsService;
    private final ChaosService chaosService;
    private final Random random = new Random();

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        metricsService.incrementApiCalls("info");
        chaosService.maybeInjectChaos();
        
        Map<String, Object> info = new HashMap<>();
        Map<String, Object> dynamicConfig = new HashMap<>();
        info.put("appName", appConfig.getAppName());
        info.put("customerName", appConfig.getDisplayCustomerName());
        info.put("version", appConfig.getDisplayVersion());
        info.put("rawVersion", appConfig.getVersion());
        info.put("buildId", appConfig.getDisplayBuildId());
        info.put("environment", appConfig.getEnvironment());
        info.put("deploymentTarget", appConfig.getDeploymentTarget());
        info.put("deploymentTargetDisplay", appConfig.getDisplayDeploymentTarget());
        info.put("deploymentVariant", appConfig.getEffectiveVariant());
        info.put("deploymentTrack", appConfig.getDeploymentTrack());
        info.put("deploymentStrategy", appConfig.getEffectiveDeploymentStrategy());
        info.put("deploymentStrategyDisplay", appConfig.getDisplayDeploymentStrategy());
        info.put("deploymentNarrative", appConfig.getDeploymentNarrative());
        info.put("variantColor", appConfig.getVariantColor());
        info.put("hostname", getHostname());
        info.put("podName", appConfig.getPodName());
        info.put("namespace", appConfig.getNamespace());
        info.put("region", appConfig.getRegion());
        info.put("publicUrl", appConfig.getPublicUrl());
        info.put("stageUrl", appConfig.getStageUrl());
        info.put("configProfile", appConfig.getConfigProfile());
        info.put("configBanner", appConfig.getDisplayConfigBanner());
        info.put("configTenant", appConfig.getConfigTenant());
        info.put("configReleaseRing", appConfig.getConfigReleaseRing());
        info.put("configTarget", appConfig.getDisplayConfigTarget());
        info.put("configSupportContact", appConfig.getConfigSupportContact());
        info.put("configVersion", appConfig.getDisplayConfigVersion());
        info.put("configSource", appConfig.getDisplayConfigSource());
        info.put("secretProvider", appConfig.getDisplaySecretProvider());
        info.put("dynamicSecretConfigured", appConfig.isDynamicSecretConfigured());
        dynamicConfig.put("profile", appConfig.getConfigProfile());
        dynamicConfig.put("banner", appConfig.getDisplayConfigBanner());
        dynamicConfig.put("tenant", appConfig.getConfigTenant());
        dynamicConfig.put("releaseRing", appConfig.getConfigReleaseRing());
        dynamicConfig.put("target", appConfig.getDisplayConfigTarget());
        dynamicConfig.put("supportContact", appConfig.getConfigSupportContact());
        dynamicConfig.put("version", appConfig.getDisplayConfigVersion());
        dynamicConfig.put("source", appConfig.getDisplayConfigSource());
        dynamicConfig.put("secretProvider", appConfig.getDisplaySecretProvider());
        dynamicConfig.put("secretConfigured", appConfig.isDynamicSecretConfigured());
        dynamicConfig.put("artifactPromotionModel", "same-artifact-different-config");
        dynamicConfig.put("injectionModel", "deployment-time-config-injection");
        info.put("dynamicConfig", dynamicConfig);
        info.put("timestamp", Instant.now().toString());
        info.put("javaVersion", System.getProperty("java.version"));
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        metricsService.incrementApiCalls("health");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", appConfig.getDisplayVersion());
        health.put("rawVersion", appConfig.getVersion());
        health.put("deploymentVariant", appConfig.getEffectiveVariant());
        health.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        metricsService.incrementApiCalls("version");
        
        Map<String, String> version = new HashMap<>();
        version.put("version", appConfig.getDisplayVersion());
        version.put("rawVersion", appConfig.getVersion());
        version.put("variant", appConfig.getEffectiveVariant());
        version.put("color", appConfig.getVariantColor());
        
        return ResponseEntity.ok(version);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestBody(required = false) Map<String, Object> payload) {
        metricsService.incrementApiCalls("process");
        metricsService.recordProcessingTime(random.nextInt(100) + 50);
        chaosService.maybeInjectChaos();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "processed");
        response.put("processedBy", getHostname());
        response.put("variant", appConfig.getEffectiveVariant());
        response.put("timestamp", Instant.now().toString());
        response.put("inputReceived", payload != null);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/simulate-load")
    public ResponseEntity<Map<String, Object>> simulateLoad(
            @RequestParam(defaultValue = "100") int durationMs) {
        
        metricsService.incrementApiCalls("simulate-load");
        
        long start = System.currentTimeMillis();
        
        // Simulate CPU work
        @SuppressWarnings("unused")
        double result = 0;
        while (System.currentTimeMillis() - start < durationMs) {
            result += Math.sqrt(random.nextDouble());
        }
        
        long actualDuration = System.currentTimeMillis() - start;
        metricsService.recordProcessingTime(actualDuration);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestedDurationMs", durationMs);
        response.put("actualDurationMs", actualDuration);
        response.put("processedBy", getHostname());
        response.put("variant", appConfig.getEffectiveVariant());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/simulate-memory")
    public ResponseEntity<Map<String, Object>> simulateMemory(
            @RequestParam(defaultValue = "10") int sizeMb) {
        
        metricsService.incrementApiCalls("simulate-memory");
        
        // Allocate memory temporarily
        byte[][] memory = new byte[sizeMb][1024 * 1024];
        for (int i = 0; i < sizeMb; i++) {
            random.nextBytes(memory[i]);
        }
        
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> response = new HashMap<>();
        response.put("allocatedMb", sizeMb);
        response.put("totalMemoryMb", runtime.totalMemory() / (1024 * 1024));
        response.put("freeMemoryMb", runtime.freeMemory() / (1024 * 1024));
        response.put("usedMemoryMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        response.put("processedBy", getHostname());
        
        return ResponseEntity.ok(response);
    }

    // ==================== CV-TRIGGERABLE CHAOS ENDPOINTS ====================
    // These endpoints can be called by Harness pipelines to inject chaos during
    // canary deployments, causing metrics to degrade so Continuous Verification
    // detects the issue and triggers a rollback.

    @PostMapping("/chaos/enable")
    public ResponseEntity<Map<String, Object>> enableChaos(
            @RequestParam(defaultValue = "500") int latencyMs,
            @RequestParam(defaultValue = "0.3") double errorRate,
            @RequestParam(defaultValue = "60") int durationSeconds) {
        
        metricsService.incrementChaosEvents("enable");
        
        // Store chaos config - this will affect all subsequent requests
        chaosService.enableChaos(latencyMs, errorRate, durationSeconds);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "chaos_enabled");
        response.put("latencyMs", latencyMs);
        response.put("errorRate", errorRate);
        response.put("durationSeconds", durationSeconds);
        response.put("variant", appConfig.getEffectiveVariant());
        response.put("message", "Chaos injection enabled - CV should detect degraded metrics");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chaos/disable")
    public ResponseEntity<Map<String, Object>> disableChaos() {
        metricsService.incrementChaosEvents("disable");
        chaosService.disableChaos();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "chaos_disabled");
        response.put("variant", appConfig.getEffectiveVariant());
        response.put("message", "Chaos injection disabled - metrics should return to normal");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chaos/status")
    public ResponseEntity<Map<String, Object>> chaosStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", chaosService.isChaosActive());
        response.put("latencyMs", chaosService.getCurrentLatency());
        response.put("errorRate", chaosService.getCurrentErrorRate());
        response.put("remainingSeconds", chaosService.getRemainingSeconds());
        response.put("variant", appConfig.getEffectiveVariant());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chaos/degrade-canary")
    public ResponseEntity<Map<String, Object>> degradeCanary(
            @RequestParam(defaultValue = "60") int durationSeconds) {
        
        // Only degrade if this is a canary deployment
        if (!"canary".equalsIgnoreCase(appConfig.getEffectiveVariant())) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "skipped");
            response.put("variant", appConfig.getEffectiveVariant());
            response.put("message", "Not a canary deployment - chaos not injected");
            return ResponseEntity.ok(response);
        }
        
        metricsService.incrementChaosEvents("degrade-canary");
        
        // Enable aggressive chaos to trigger CV rollback
        chaosService.enableChaos(800, 0.5, durationSeconds);
        
        // Generate some immediate errors for CV to detect
        for (int i = 0; i < 10; i++) {
            metricsService.incrementErrors("canary_degradation");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "canary_degraded");
        response.put("latencyMs", 800);
        response.put("errorRate", 0.5);
        response.put("durationSeconds", durationSeconds);
        response.put("variant", appConfig.getEffectiveVariant());
        response.put("message", "Canary deployment degraded - CV should detect and trigger rollback");
        
        return ResponseEntity.ok(response);
    }

    private String getHostname() {
        try {
            String podName = System.getenv("HOSTNAME");
            if (podName != null && !podName.isEmpty()) {
                return podName;
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
