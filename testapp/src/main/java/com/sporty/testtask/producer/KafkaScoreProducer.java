package com.sporty.testtask.producer;

import com.sporty.testtask.avro.ScoreEvent;
import com.sporty.testtask.config.DomainProperties;
import com.sporty.testtask.dto.ScoreMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaScoreProducer {

    private final KafkaTemplate<String, ScoreEvent> kafkaTemplate;
    private final DomainProperties domainProperties;

    public void send(ScoreMessage message) {
        ScoreEvent event = ScoreEvent.newBuilder()
                .setEventId(message.getEventId())
                .setCurrentScore(message.getCurrentScore())
                .setTimestamp(message.getTimestamp())
                .build();

        String topic = domainProperties.getKafka().getScoreUpdatesTopic();
        kafkaTemplate.send(topic, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ScoreEvent for eventId={}: {}",
                                message.getEventId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published ScoreEvent for eventId={} to topic={} offset={}",
                                message.getEventId(), topic, result.getRecordMetadata().offset());
                    }
                });
    }
}
