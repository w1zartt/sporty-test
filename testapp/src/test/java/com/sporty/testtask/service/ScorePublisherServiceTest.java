package com.sporty.testtask.service;

import com.sporty.testtask.client.SportEventClient;
import com.sporty.testtask.dto.ScoreMessage;
import com.sporty.testtask.dto.ScoreResponse;
import com.sporty.testtask.exception.ScoreFetchException;
import com.sporty.testtask.producer.KafkaScoreProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScorePublisherServiceTest {

    @Mock
    private SportEventClient sportEventClient;

    @Mock
    private KafkaScoreProducer kafkaScoreProducer;

    @InjectMocks
    private ScorePublisherService scorePublisherService;

    @Test
    void fetchAndPublish_success_sendsMessage() {
        ScoreResponse response = new ScoreResponse();
        response.setEventId("evt-1");
        response.setCurrentScore("1:0");
        when(sportEventClient.getScore("evt-1")).thenReturn(response);

        scorePublisherService.fetchAndPublish("evt-1");

        ArgumentCaptor<ScoreMessage> captor = ArgumentCaptor.forClass(ScoreMessage.class);
        verify(kafkaScoreProducer).send(captor.capture());
        ScoreMessage sent = captor.getValue();
        assertThat(sent.getEventId()).isEqualTo("evt-1");
        assertThat(sent.getCurrentScore()).isEqualTo("1:0");
        assertThat(sent.getTimestamp()).isNotNull();
    }

    @Test
    void fetchAndPublish_clientThrows_throwsScoreFetchException() {
        when(sportEventClient.getScore("evt-1")).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> scorePublisherService.fetchAndPublish("evt-1"))
                .isInstanceOf(ScoreFetchException.class)
                .hasMessageContaining("evt-1");

        verifyNoInteractions(kafkaScoreProducer);
    }
}
