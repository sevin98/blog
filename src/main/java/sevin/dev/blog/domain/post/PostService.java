package sevin.dev.blog.domain.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sevin.dev.blog.common.exception.BlogException;
import sevin.dev.blog.common.exception.ErrorCode;
import sevin.dev.blog.domain.post.dto.CreatePostRequest;
import sevin.dev.blog.domain.post.dto.PostResponse;
import sevin.dev.blog.domain.post.dto.UpdatePostRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public PostResponse create(CreatePostRequest request) {
        String slug = generateSlug(request.title());
        Post post = Post.builder()
                .title(request.title())
                .slug(slug)
                .content(request.content())
                .status(PostStatus.DRAFT)
                .build();
        try {
            return PostResponse.from(postRepository.save(post));
        } catch (DataIntegrityViolationException e) {
            log.warn("Slug collision on save, retrying with UUID fallback");
            post = Post.builder()
                    .title(request.title())
                    .slug(slug + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .content(request.content())
                    .status(PostStatus.DRAFT)
                    .build();
            return PostResponse.from(postRepository.save(post));
        }
    }

    public Page<PostResponse> findAll(PostStatus status, Pageable pageable) {
        if (status != null) {
            return postRepository.findByStatus(status, pageable).map(PostResponse::from);
        }
        return postRepository.findAll(pageable).map(PostResponse::from);
    }

    public PostResponse findBySlug(String slug) {
        return postRepository.findBySlug(slug)
                .map(PostResponse::from)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));
    }

    @Transactional
    public PostResponse update(String slug, UpdatePostRequest request) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));
        post.update(request.title(), request.content());
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse publish(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));
        post.publish();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));
        post.delete();
    }

    private String generateSlug(String title) {
        String base = title.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9가-힣\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (base.isEmpty()) {
            base = UUID.randomUUID().toString().substring(0, 8);
        }

        if (!postRepository.existsBySlug(base)) {
            return base;
        }

        int suffix = 2;
        while (postRepository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
