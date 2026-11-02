package rapport.probe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthProbeTest {
    private final HealthProbe probe = new HealthProbe();

    @Test
    void http200IsUp() {
        ProbeResult result = probe.interpret("api", 200, 15);

        assertTrue(result.up());
        assertEquals("ok", result.detail());
        assertEquals(15, result.latencyMillis());
    }

    @Test
    void http500IsDown() {
        ProbeResult result = probe.interpret("api", 500, 40);

        assertFalse(result.up());
        assertEquals("http 500", result.detail());
        assertEquals(40, result.latencyMillis());
    }
}
