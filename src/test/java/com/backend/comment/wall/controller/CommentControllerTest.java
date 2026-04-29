package com.backend.comment.wall.controller;

import com.backend.comment.wall.dto.CommentRequest;
import com.backend.comment.wall.dto.CommentResponse;
import com.backend.comment.wall.exception.CommentValidationException;
import com.backend.comment.wall.exception.RateLimitExceededException;
import com.backend.comment.wall.service.CommentService;
import com.backend.comment.wall.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@Import(com.backend.comment.wall.exception.GlobalExceptionHandler.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void shouldReturnCommentsInResponseBody() throws Exception {
        when(commentService.getAllComments()).thenReturn(List.of(
                new CommentResponse(2L, "Nova", "Latest note", LocalDateTime.parse("2026-04-11T10:15:00")),
                new CommentResponse(1L, "Sky", "Earlier note", LocalDateTime.parse("2026-04-11T10:00:00"))
        ));

        mockMvc.perform(get("/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Nova"))
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void shouldCreateCommentWhenPayloadIsValid() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setName("Nova");
        request.setContent("Hello wall");

        when(commentService.saveComment(any(CommentRequest.class))).thenReturn(
                new CommentResponse(3L, "Nova", "Hello wall", LocalDateTime.parse("2026-04-11T10:20:00"))
        );
        doNothing().when(rateLimiterService).validateRequest(eq("127.0.0.1"));

        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.content").value("Hello wall"));

        verify(rateLimiterService).validateRequest("127.0.0.1");
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setName("");
        request.setContent("");

        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed."))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturnBadRequestWhenProfanityIsDetected() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setName("Nova");
        request.setContent("This is stupid");

        doNothing().when(rateLimiterService).validateRequest(eq("127.0.0.1"));
        when(commentService.saveComment(any(CommentRequest.class)))
                .thenThrow(new CommentValidationException("Comment contains prohibited language."));

        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Comment contains prohibited language."));
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setName("Nova");
        request.setContent("Trying again");

        doThrow(new RateLimitExceededException("Rate limit exceeded. Maximum 5 requests per minute."))
                .when(rateLimiterService)
                .validateRequest(eq("127.0.0.1"));

        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded. Maximum 5 requests per minute."));
    }
}
