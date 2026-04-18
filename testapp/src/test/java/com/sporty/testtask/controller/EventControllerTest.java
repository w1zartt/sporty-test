package com.sporty.testtask.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sporty.testtask.dto.EventStatusRequest;
import com.sporty.testtask.exception.GlobalExceptionHandler;
import com.sporty.testtask.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = {EventController.class, GlobalExceptionHandler.class})
class EventControllerTest {

    private static final String UPDATE_STATUS_URL = "/events/status";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @Test
    void updateStatus_live_returnsOk() throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("evt-1");
        request.setLive(true);

        mockMvc.perform(post(UPDATE_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(eventService).updateStatus("evt-1", true);
    }

    @Test
    void updateStatus_notLive_returnsOk() throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("evt-1");
        request.setLive(false);

        mockMvc.perform(post(UPDATE_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(eventService).updateStatus("evt-1", false);
    }

    @Test
    void updateStatus_missingEventId_returns400() throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setLive(true);

        mockMvc.perform(post(UPDATE_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void updateStatus_missingLive_returns400() throws Exception {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("evt-1");

        mockMvc.perform(post(UPDATE_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }
}
