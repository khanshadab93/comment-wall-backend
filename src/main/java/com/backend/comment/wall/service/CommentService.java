package com.backend.comment.wall.service;

import com.backend.comment.wall.dto.CommentRequest;
import com.backend.comment.wall.dto.CommentResponse;
import com.backend.comment.wall.entity.Comment;
import com.backend.comment.wall.exception.CommentValidationException;
import com.backend.comment.wall.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CommentService {

    private static final Set<String> BANNED_WORDS = Set.of(
            "damn",
            "hell",
            "idiot",
            "stupid",
            "trash"
    );

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentResponse saveComment(CommentRequest request) {
        String sanitizedName = normalize(request.getName());
        String sanitizedContent = normalize(request.getContent());

        validateProfanity(sanitizedName, "Name");
        validateProfanity(sanitizedContent, "Comment");

        Comment comment = new Comment();
        comment.setName(sanitizedName);
        comment.setContent(sanitizedContent);

        Comment saved = commentRepository.save(comment);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getAllComments() {
        return commentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateProfanity(String value, String fieldName) {
        String normalized = value.toLowerCase(Locale.ROOT);
        boolean containsBannedWord = BANNED_WORDS.stream().anyMatch(normalized::contains);
        if (containsBannedWord) {
            throw new CommentValidationException(fieldName + " contains prohibited language.");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private CommentResponse mapToResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
