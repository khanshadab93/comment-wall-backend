package com.backend.comment.wall.exception;

public class CommentValidationException extends RuntimeException {

    public CommentValidationException(String message) {
        super(message);
    }
}
