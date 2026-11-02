package rapport.probe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    private final HealthProbe healthProbe;

    public StatusController(HealthProbe healthProbe) {
        this.healthProbe = healthProbe;
    }

    @GetMapping("/status")
    public ProbeResult status() {
        return healthProbe.interpret("api", 200, 0);
    }
}
