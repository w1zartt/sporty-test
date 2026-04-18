package com.sporty.testtask.exception;

public class ScoreFetchException extends RuntimeException {

    public ScoreFetchException(String eventId, Throwable cause) {
        super("Failed to fetch score for eventId=" + eventId, cause);
    }
}
