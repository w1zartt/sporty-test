package com.sporty.testtask.producer;

import com.sporty.testtask.avro.ScoreEvent;
import com.sporty.testtask.config.DomainProperties;
import com.sporty.testtask.dto.ScoreMessage;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaScoreProducerTest {

    private static final String EVENT_ID = "evt-1";
    private static final String TOPIC = "score-updates";

    @Mock
    private KafkaTemplate<String, ScoreEvent> kafkaTemplate;

    private DomainProperties domainProperties;
    private KafkaScoreProducer kafkaScoreProducer;

    @BeforeEach
    void setUp() {
        domainProperties = new DomainProperties();
        var kafka = new DomainProperties.Kafka();
        kafka.setScoreUpdatesTopic(TOPIC);
        domainProperties.setKafka(kafka);
        kafkaScoreProducer = new KafkaScoreProducer(kafkaTemplate, domainProperties);
    }

    @Test
    void send_success_publishesAvroEvent() {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 0, 0, 0, 0, 0);
        SendResult<String, ScoreEvent> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq(TOPIC), eq(EVENT_ID), any(ScoreEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        ScoreMessage message = ScoreMessage.builder()
                .eventId(EVENT_ID)
                .currentScore("2:1")
                .timestamp(Instant.now())
                .build();

        kafkaScoreProducer.send(message);

        ArgumentCaptor<ScoreEvent> captor = ArgumentCaptor.forClass(ScoreEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(EVENT_ID), captor.capture());
        ScoreEvent event = captor.getValue();
        assertThat(event.getEventId()).isEqualTo(EVENT_ID);
        assertThat(event.getCurrentScore()).isEqualTo("2:1");
    }

    @Test
    void send_kafkaFails_logsErrorWithoutThrowing() {
        CompletableFuture<SendResult<String, ScoreEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        ScoreMessage message = ScoreMessage.builder()
                .eventId(EVENT_ID)
                .currentScore("0:0")
                .timestamp(Instant.now())
                .build();

        // no exception thrown — error is handled in whenComplete callback
        kafkaScoreProducer.send(message);
    }
}
