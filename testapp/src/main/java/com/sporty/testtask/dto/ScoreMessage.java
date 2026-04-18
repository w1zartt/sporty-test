package com.sporty.testtask.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ScoreMessage {

    private String eventId;
    private String currentScore;
    private Instant timestamp;
}
