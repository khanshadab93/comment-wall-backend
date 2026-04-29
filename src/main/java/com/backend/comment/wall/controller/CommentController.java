package com.backend.comment.wall.controller;

import com.backend.comment.wall.dto.CommentRequest;
import com.backend.comment.wall.dto.CommentResponse;
import com.backend.comment.wall.service.CommentService;
import com.backend.comment.wall.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final RateLimiterService rateLimiterService;

    public CommentController(CommentService commentService, RateLimiterService rateLimiterService) {
        this.commentService = commentService;
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping
    public List<CommentResponse> getComments() {
        return commentService.getAllComments();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@Valid @RequestBody CommentRequest commentRequest, HttpServletRequest request) {
        rateLimiterService.validateRequest(resolveClientIp(request));
        return commentService.saveComment(commentRequest);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
