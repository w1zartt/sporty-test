package com.sporty.testtask.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "spring.application.domain-properties")
public class DomainProperties {

    private SportEventsSource sportEventsSource = new SportEventsSource();
    private Kafka kafka = new Kafka();
    private Duration pollInterval = Duration.ofSeconds(10);

    @Data
    public static class SportEventsSource {
        private String url;
    }

    @Data
    public static class Kafka {
        private String scoreUpdatesTopic = "score-updates";
    }
}
