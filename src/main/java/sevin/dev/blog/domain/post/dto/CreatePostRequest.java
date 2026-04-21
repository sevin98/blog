package sevin.dev.blog.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import sevin.dev.blog.domain.post.PostStatus;

public record CreatePostRequest(
        @NotBlank @Size(max = 500) String title,
        @NotBlank String content,
        PostStatus status
) {}
