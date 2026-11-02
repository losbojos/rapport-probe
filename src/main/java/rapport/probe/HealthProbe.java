package rapport.probe;

public final class HealthProbe {
    public ProbeResult interpret(String name, int statusCode, long latencyMillis) {
        boolean up = statusCode >= 200 && statusCode < 300;
        String detail = up ? "ok" : "http " + statusCode;
        return new ProbeResult(name, up, latencyMillis, detail);
    }
}
