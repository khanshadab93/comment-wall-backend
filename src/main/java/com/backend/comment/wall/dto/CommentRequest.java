package com.backend.comment.wall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {

    @NotBlank(message = "Name is required.")
    @Size(max = 50, message = "Name must be at most 50 characters.")
    private String name;

    @NotBlank(message = "Comment is required.")
    @Size(max = 300, message = "Comment must be at most 300 characters.")
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
