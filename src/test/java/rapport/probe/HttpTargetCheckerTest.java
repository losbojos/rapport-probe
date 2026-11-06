package rapport.probe;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTargetCheckerTest {
    private final HttpTargetChecker checker = new HttpTargetChecker(
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
            new HealthProbe()
    );

    @Test
    void http200IsUp() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/health", exchange -> {
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            ProbeResult result = checker.check(new ProbeProperties.Target(
                    "api",
                    "http://127.0.0.1:" + port + "/api/health"
            ));

            assertTrue(result.up());
            assertEquals("api", result.name());
            assertEquals("ok", result.detail());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void closedPortIsDown() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        ProbeResult result = checker.check(new ProbeProperties.Target(
                "api",
                "http://127.0.0.1:" + port + "/api/health"
        ));

        assertFalse(result.up());
        assertEquals("unreachable", result.detail());
    }
}
