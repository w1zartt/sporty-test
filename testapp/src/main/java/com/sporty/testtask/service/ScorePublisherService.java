package com.sporty.testtask.service;

import com.sporty.testtask.client.SportEventClient;
import com.sporty.testtask.dto.ScoreMessage;
import com.sporty.testtask.dto.ScoreResponse;
import com.sporty.testtask.exception.ScoreFetchException;
import com.sporty.testtask.producer.KafkaScoreProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScorePublisherService {

    private final SportEventClient sportEventClient;
    private final KafkaScoreProducer kafkaScoreProducer;

    public void fetchAndPublish(String eventId) {
        ScoreResponse score = fetchScore(eventId);

        ScoreMessage message = ScoreMessage.builder()
                .eventId(score.getEventId())
                .currentScore(score.getCurrentScore())
                .timestamp(Instant.now())
                .build();

        kafkaScoreProducer.send(message);
    }

    private ScoreResponse fetchScore(String eventId) {
        try {
            ScoreResponse response = sportEventClient.getScore(eventId);
            log.debug("Fetched score for eventId={}: {}", eventId, response.getCurrentScore());
            return response;
        } catch (Exception e) {
            throw new ScoreFetchException(eventId, e);
        }
    }
}
