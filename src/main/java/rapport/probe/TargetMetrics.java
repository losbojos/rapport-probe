package rapport.probe;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class TargetMetrics {
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> upByTarget = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<Double>> latencyByTarget = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<Double>> lastSuccessByTarget = new ConcurrentHashMap<>();

    public TargetMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(ProbeResult result) {
        AtomicInteger up = upByTarget.computeIfAbsent(result.name(), this::registerUp);
        AtomicReference<Double> latency = latencyByTarget.computeIfAbsent(result.name(), this::registerLatency);
        AtomicReference<Double> lastSuccess = lastSuccessByTarget.computeIfAbsent(result.name(), this::registerLastSuccess);

        up.set(result.up() ? 1 : 0);
        latency.set(result.latencyMillis() / 1000.0);
        if (result.up()) {
            lastSuccess.set(System.currentTimeMillis() / 1000.0);
        }
    }

    private AtomicInteger registerUp(String target) {
        AtomicInteger value = new AtomicInteger(0);
        Gauge.builder("rapport_target_up", value, AtomicInteger::get)
                .tag("target", target)
                .description("1 if the target answered with HTTP 2xx, otherwise 0")
                .register(meterRegistry);
        return value;
    }

    private AtomicReference<Double> registerLatency(String target) {
        AtomicReference<Double> value = new AtomicReference<>(0.0);
        Gauge.builder("rapport_target_latency_seconds", value, AtomicReference::get)
                .tag("target", target)
                .description("Duration of the latest probe request in seconds")
                .register(meterRegistry);
        return value;
    }

    private AtomicReference<Double> registerLastSuccess(String target) {
        AtomicReference<Double> value = new AtomicReference<>(0.0);
        Gauge.builder("rapport_target_last_success_timestamp_seconds", value, AtomicReference::get)
                .tag("target", target)
                .description("Unix time of the latest successful probe")
                .register(meterRegistry);
        return value;
    }
}
