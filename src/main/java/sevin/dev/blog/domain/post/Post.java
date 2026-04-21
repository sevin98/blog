package sevin.dev.blog.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import sevin.dev.blog.common.exception.BlogException;
import sevin.dev.blog.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, unique = true, length = 500)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void update(String title, String content, PostStatus status) {
        if (this.status == PostStatus.DELETED) {
            throw new BlogException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (status == PostStatus.DELETED) {
            throw new BlogException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public void delete() {
        this.status = PostStatus.DELETED;
    }
}
