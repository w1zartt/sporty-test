package com.sporty.testtask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sporty.testtask.avro.ScoreEvent;
import com.sporty.testtask.client.SportEventClient;
import com.sporty.testtask.dto.EventStatusRequest;
import com.sporty.testtask.dto.ScoreResponse;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.application.domain-properties.poll-interval=1s")
class EventE2ETest extends BaseIntegrationTest {

    private static final String TOPIC = "score-updates";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SportEventClient sportEventClient;

    @AfterEach
    void tearDown() {
        reset(sportEventClient);
    }

    @Test
    void shouldPublishScoreEventToKafkaWhenEventGoesLive() throws Exception {
        String eventId = "event-001";

        ScoreResponse score = new ScoreResponse();
        score.setEventId(eventId);
        score.setCurrentScore("2:1");
        when(sportEventClient.getScore(eventId)).thenReturn(score);

        postStatus(eventId, true);

        List<ConsumerRecord<String, ScoreEvent>> records = pollKafka(TOPIC, eventId, Duration.ofSeconds(15));
        assertThat(records).isNotEmpty();
        ScoreEvent published = records.get(0).value();
        assertThat(published.getEventId()).isEqualTo(eventId);
        assertThat(published.getCurrentScore()).isEqualTo("2:1");

        postStatus(eventId, false);
    }

    @Test
    void shouldIgnoreDuplicateLiveRequest() throws Exception {
        String eventId = "event-003";

        ScoreResponse score = new ScoreResponse();
        score.setEventId(eventId);
        score.setCurrentScore("1:0");
        when(sportEventClient.getScore(eventId)).thenReturn(score);

        postStatus(eventId, true);
        postStatus(eventId, true); // duplicate — must not throw

        List<ConsumerRecord<String, ScoreEvent>> records = pollKafka(TOPIC, eventId, Duration.ofSeconds(15));
        assertThat(records).isNotEmpty();

        postStatus(eventId, false);
    }

    @Test
    void shouldReturnBadRequestWhenEventIdIsBlank() throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("");
        request.setLive(true);

        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenLiveIsMissing() throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("event-x");
        // live intentionally not set — null triggers @NotNull

        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    private void postStatus(String eventId, boolean live) throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId(eventId);
        request.setLive(live);

        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private List<ConsumerRecord<String, ScoreEvent>> pollKafka(String topic, String key, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-consumer-" + key + "-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url",
                "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getMappedPort(8081));
        props.put("specific.avro.reader", "true");

        try (KafkaConsumer<String, ScoreEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, ScoreEvent> batch = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, ScoreEvent> record : batch) {
                    if (key.equals(record.key())) {
                        return List.of(record);
                    }
                }
            }
        }
        return Collections.emptyList();
    }
}
