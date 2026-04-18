package com.sporty.testtask.controller;

import com.sporty.testtask.dto.EventStatusRequest;
import com.sporty.testtask.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody EventStatusRequest request) {
        log.info("Received status update: eventId={}, live={}", request.getEventId(), request.getLive());
        eventService.updateStatus(request.getEventId(), request.getLive());
        return ResponseEntity.ok().build();
    }
}
