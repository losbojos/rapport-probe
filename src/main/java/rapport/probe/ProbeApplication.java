package rapport.probe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.http.HttpClient;
import java.time.Duration;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ProbeProperties.class)
public class ProbeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProbeApplication.class, args);
    }

    @Bean
    HealthProbe healthProbe() {
        return new HealthProbe();
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Bean
    HttpTargetChecker httpTargetChecker(HttpClient httpClient, HealthProbe healthProbe) {
        return new HttpTargetChecker(httpClient, healthProbe);
    }

    @Bean
    ProbeSnapshot probeSnapshot() {
        return new ProbeSnapshot();
    }

    @Bean
    ProbeScheduler probeScheduler(
            ProbeProperties properties,
            HttpTargetChecker checker,
            ProbeSnapshot snapshot
    ) {
        return new ProbeScheduler(properties, checker, snapshot);
    }
}
