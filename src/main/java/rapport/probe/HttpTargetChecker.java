package rapport.probe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class HttpTargetChecker {
    private final HttpClient httpClient;
    private final HealthProbe healthProbe;

    public HttpTargetChecker(HttpClient httpClient, HealthProbe healthProbe) {
        this.httpClient = httpClient;
        this.healthProbe = healthProbe;
    }

    public ProbeResult check(ProbeProperties.Target target) {
        long started = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target.url()))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return healthProbe.interpret(target.name(), response.statusCode(), elapsed(started));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unreachable(target.name(), started);
        } catch (IOException | IllegalArgumentException exception) {
            return unreachable(target.name(), started);
        }
    }

    private ProbeResult unreachable(String name, long started) {
        return new ProbeResult(name, false, elapsed(started), "unreachable");
    }

    private static long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}
