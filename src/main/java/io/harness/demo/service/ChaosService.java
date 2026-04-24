package io.harness.demo.service;

import io.harness.demo.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ChaosService {

    private final AppConfig appConfig;
    private final MetricsService metricsService;
    private final Random random = new Random();

    // Dynamic chaos state (can be enabled/disabled via API for CV testing)
    private final AtomicBoolean dynamicChaosEnabled = new AtomicBoolean(false);
    private final AtomicInteger dynamicLatencyMs = new AtomicInteger(0);
    private final AtomicReference<Double> dynamicErrorRate = new AtomicReference<>(0.0);
    private final AtomicLong chaosExpirationTime = new AtomicLong(0);

    public void maybeInjectChaos() {
        // Check if dynamic chaos is active (from API trigger)
        if (isChaosActive()) {
            injectDynamicChaos();
            return;
        }

        // Fall back to config-based chaos
        if (!appConfig.isChaosEnabled()) {
            return;
        }

        // Inject latency from config
        if (appConfig.getChaosLatencyMs() > 0) {
            try {
                Thread.sleep(appConfig.getChaosLatencyMs());
                metricsService.incrementChaosEvents("latency");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Inject errors from config
        if (appConfig.getChaosErrorRate() > 0 && random.nextDouble() < appConfig.getChaosErrorRate()) {
            metricsService.incrementChaosEvents("error");
            metricsService.incrementErrors("chaos_injected");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chaos Engineering: Injected failure");
        }
    }

    private void injectDynamicChaos() {
        int latency = dynamicLatencyMs.get();
        double errorRate = dynamicErrorRate.get();

        // Inject latency
        if (latency > 0) {
            try {
                Thread.sleep(latency);
                metricsService.incrementChaosEvents("cv_latency");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Inject errors based on error rate
        if (errorRate > 0 && random.nextDouble() < errorRate) {
            metricsService.incrementChaosEvents("cv_error");
            metricsService.incrementErrors("cv_chaos_injected");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "CV Chaos: Simulated failure for Continuous Verification testing");
        }
    }

    // ==================== CV-TRIGGERABLE CHAOS METHODS ====================

    public void enableChaos(int latencyMs, double errorRate, int durationSeconds) {
        dynamicLatencyMs.set(latencyMs);
        dynamicErrorRate.set(errorRate);
        chaosExpirationTime.set(Instant.now().plusSeconds(durationSeconds).toEpochMilli());
        dynamicChaosEnabled.set(true);
        metricsService.incrementChaosEvents("cv_enabled");
    }

    public void disableChaos() {
        dynamicChaosEnabled.set(false);
        dynamicLatencyMs.set(0);
        dynamicErrorRate.set(0.0);
        chaosExpirationTime.set(0);
        metricsService.incrementChaosEvents("cv_disabled");
    }

    public boolean isChaosActive() {
        if (!dynamicChaosEnabled.get()) {
            return false;
        }
        // Check if chaos has expired
        long expiration = chaosExpirationTime.get();
        if (expiration > 0 && System.currentTimeMillis() > expiration) {
            disableChaos();
            return false;
        }
        return true;
    }

    public int getCurrentLatency() {
        return isChaosActive() ? dynamicLatencyMs.get() : 0;
    }

    public double getCurrentErrorRate() {
        return isChaosActive() ? dynamicErrorRate.get() : 0.0;
    }

    public long getRemainingSeconds() {
        if (!isChaosActive()) {
            return 0;
        }
        long remaining = (chaosExpirationTime.get() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    // ==================== MANUAL CHAOS METHODS ====================

    public void injectLatency(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
            metricsService.incrementChaosEvents("latency_manual");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void injectError() {
        metricsService.incrementChaosEvents("error_manual");
        metricsService.incrementErrors("chaos_manual");
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chaos Engineering: Manual failure injection");
    }
}
