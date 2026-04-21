package sevin.dev.blog.domain.post.dto;

import sevin.dev.blog.domain.post.Post;
import sevin.dev.blog.domain.post.PostStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
        Long id,
        UUID uuid,
        String title,
        String slug,
        String content,
        PostStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getUuid(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getStatus(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
