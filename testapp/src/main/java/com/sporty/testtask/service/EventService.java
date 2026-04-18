package com.sporty.testtask.service;

import com.sporty.testtask.config.DomainProperties;
import com.sporty.testtask.exception.ScoreFetchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final TaskScheduler taskScheduler;
    private final ScorePublisherService scorePublisherService;
    private final DomainProperties domainProperties;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void updateStatus(String eventId, boolean live) {
        if (live) {
            startPolling(eventId);
        } else {
            stopPolling(eventId);
        }
    }

    private void startPolling(String eventId) {
        if (scheduledTasks.containsKey(eventId)) {
            log.info("Event {} is already being polled, ignoring duplicate live request", eventId);
            return;
        }

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        scorePublisherService.fetchAndPublish(eventId);
                    } catch (ScoreFetchException e) {
                        log.error("Score fetch error for eventId={}: {}", eventId, e.getMessage(), e);
                    }
                },
                domainProperties.getPollInterval()
        );
        scheduledTasks.put(eventId, future);
        log.info("Started polling for event {}", eventId);
    }

    private void stopPolling(String eventId) {
        ScheduledFuture<?> future = scheduledTasks.remove(eventId);
        if (future != null) {
            future.cancel(false);
            log.info("Stopped polling for event {}", eventId);
        } else {
            log.info("No active polling found for event {}", eventId);
        }
    }
}
