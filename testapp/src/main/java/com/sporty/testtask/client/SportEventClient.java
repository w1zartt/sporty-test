package com.sporty.testtask.client;

import com.sporty.testtask.dto.ScoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "sport-events-source",
        url = "${spring.application.domain-properties.sport-events-source.url}"
)
public interface SportEventClient {

    @GetMapping("/events/{eventId}/score")
    ScoreResponse getScore(@PathVariable("eventId") String eventId);
}
