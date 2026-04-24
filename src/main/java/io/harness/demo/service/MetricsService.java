package io.harness.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void incrementPageViews(String page) {
        Counter.builder("harness_demo_page_views_total")
                .description("Total page views")
                .tag("page", page)
                .register(meterRegistry)
                .increment();
    }

    public void incrementApiCalls(String endpoint) {
        Counter.builder("harness_demo_api_calls_total")
                .description("Total API calls")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }

    public void recordProcessingTime(long durationMs) {
        Timer.builder("harness_demo_processing_duration")
                .description("Processing duration in milliseconds")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void incrementChaosEvents(String type) {
        Counter.builder("harness_demo_chaos_events_total")
                .description("Total chaos events injected")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }

    public void incrementErrors(String type) {
        Counter.builder("harness_demo_errors_total")
                .description("Total errors")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }
}
