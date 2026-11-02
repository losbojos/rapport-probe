package rapport.probe;

public record ProbeResult(String name, boolean up, long latencyMillis, String detail) {}
