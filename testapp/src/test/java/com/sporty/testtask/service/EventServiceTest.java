package com.sporty.testtask.service;

import com.sporty.testtask.config.DomainProperties;
import com.sporty.testtask.exception.ScoreFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScorePublisherService scorePublisherService;

    @Mock
    private DomainProperties domainProperties;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(taskScheduler, scorePublisherService, domainProperties);
    }

    private void stubScheduling() {
        when(domainProperties.getPollInterval()).thenReturn(Duration.ofSeconds(10));
        doReturn(scheduledFuture).when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    void updateStatus_live_schedulesTask() {
        stubScheduling();
        eventService.updateStatus("evt-1", true);

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));
    }

    @Test
    void updateStatus_duplicateLive_schedulesOnlyOnce() {
        stubScheduling();
        eventService.updateStatus("evt-1", true);
        eventService.updateStatus("evt-1", true);

        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    void updateStatus_notLive_cancelsTask() {
        stubScheduling();
        eventService.updateStatus("evt-1", true);
        eventService.updateStatus("evt-1", false);

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void updateStatus_notLiveWithNoActiveTask_doesNotThrow() {
        eventService.updateStatus("evt-1", false);

        verifyNoInteractions(scheduledFuture);
    }

    @Test
    void scheduledTask_fetchThrows_doesNotPropagateException() {
        stubScheduling();
        doThrow(new ScoreFetchException("evt-1", new RuntimeException("timeout")))
                .when(scorePublisherService).fetchAndPublish("evt-1");

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        eventService.updateStatus("evt-1", true);
        verify(taskScheduler).scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class));

        runnableCaptor.getValue().run();
        // exception is swallowed by the error handler — no exception propagated
    }

    @Test
    void scheduledTask_success_callsFetchAndPublish() {
        stubScheduling();
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        eventService.updateStatus("evt-1", true);
        verify(taskScheduler).scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class));

        runnableCaptor.getValue().run();

        verify(scorePublisherService).fetchAndPublish("evt-1");
    }
}
