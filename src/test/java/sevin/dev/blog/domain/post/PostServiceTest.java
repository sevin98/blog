package sevin.dev.blog.domain.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import sevin.dev.blog.common.exception.BlogException;
import sevin.dev.blog.common.exception.ErrorCode;
import sevin.dev.blog.domain.post.dto.CreatePostRequest;
import sevin.dev.blog.domain.post.dto.PostResponse;
import sevin.dev.blog.domain.post.dto.UpdatePostRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("영문 제목 → 소문자 + 공백을 하이픈으로 변환한 slug 생성")
    void create_englishTitle_generatesSlug() {
        when(postRepository.existsBySlug("hello-world")).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse result = postService.create(new CreatePostRequest("Hello World", "content", null));

        assertThat(result.slug()).isEqualTo("hello-world");
    }

    @Test
    @DisplayName("한글 제목 → 한글 그대로 slug 생성")
    void create_koreanTitle_generatesSlug() {
        when(postRepository.existsBySlug("봄날-산책")).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse result = postService.create(new CreatePostRequest("봄날 산책", "content", null));

        assertThat(result.slug()).isEqualTo("봄날-산책");
    }

    @Test
    @DisplayName("slug 중복 시 -2 suffix 추가")
    void create_duplicateSlug_addsSuffix() {
        when(postRepository.existsBySlug("hello")).thenReturn(true);
        when(postRepository.existsBySlug("hello-2")).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse result = postService.create(new CreatePostRequest("hello", "content", null));

        assertThat(result.slug()).isEqualTo("hello-2");
    }

    @Test
    @DisplayName("status null 전달 시 DRAFT로 기본값 설정")
    void create_nullStatus_defaultsDraft() {
        when(postRepository.existsBySlug(any())).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse result = postService.create(new CreatePostRequest("title", "content", null));

        assertThat(result.status()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    @DisplayName("존재하지 않는 id 조회 시 POST_NOT_FOUND 예외")
    void findById_notFound_throwsBlogException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findById(99L))
                .isInstanceOf(BlogException.class)
                .satisfies(e -> assertThat(((BlogException) e).getErrorCode())
                        .isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    @Test
    @DisplayName("수정 시 slug 변경 없음")
    void update_titleChanged_slugUnchanged() {
        Post post = Post.builder()
                .title("old title")
                .slug("old-title")
                .content("old content")
                .status(PostStatus.DRAFT)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.update(1L, new UpdatePostRequest("new title", "new content", PostStatus.PUBLISHED));

        assertThat(post.getSlug()).isEqualTo("old-title");
        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("delete 호출 시 status가 DELETED로 변경")
    void delete_setsStatusDeleted() {
        Post post = Post.builder()
                .title("title")
                .slug("title")
                .content("content")
                .status(PostStatus.PUBLISHED)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.delete(1L);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
    }

    @Test
    @DisplayName("delete 시 존재하지 않는 id → POST_NOT_FOUND 예외")
    void delete_notFound_throwsBlogException() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(99L))
                .isInstanceOf(BlogException.class)
                .satisfies(e -> assertThat(((BlogException) e).getErrorCode())
                        .isEqualTo(ErrorCode.POST_NOT_FOUND));
    }

    @Test
    @DisplayName("findAll - status 필터 없으면 전체 조회")
    void findAll_noStatusFilter_returnsAll() {
        Post post = Post.builder()
                .title("t").slug("t").content("c").status(PostStatus.DRAFT).build();
        when(postRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        var result = postService.findAll(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(postRepository).findAll(any(PageRequest.class));
        verify(postRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("findAll - status 필터 있으면 해당 status만 조회")
    void findAll_withStatusFilter_filtersCorrectly() {
        Post post = Post.builder()
                .title("t").slug("t").content("c").status(PostStatus.PUBLISHED).build();
        when(postRepository.findByStatus(eq(PostStatus.PUBLISHED), any()))
                .thenReturn(new PageImpl<>(List.of(post)));

        var result = postService.findAll(PostStatus.PUBLISHED, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(postRepository).findByStatus(eq(PostStatus.PUBLISHED), any());
    }

    @Test
    @DisplayName("특수문자만 있는 제목 → UUID 폴백 slug 생성")
    void create_specialCharsOnlyTitle_generatesFallbackSlug() {
        when(postRepository.existsBySlug(any())).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse result = postService.create(new CreatePostRequest("!!!@@@", "content", null));

        assertThat(result.slug()).isNotEmpty();
        assertThat(result.slug()).doesNotContain("!!!");
    }

    @Test
    @DisplayName("slug 중복이 -2도 존재할 때 -3 suffix 사용")
    void create_duplicateSlugWithSuffix2_usesSuffix3() {
        when(postRepository.existsBySlug("hello")).thenReturn(true);
        when(postRepository.existsBySlug("hello-2")).thenReturn(true);
        when(postRepository.existsBySlug("hello-3")).thenReturn(false);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse result = postService.create(new CreatePostRequest("hello", "content", null));

        assertThat(result.slug()).isEqualTo("hello-3");
    }

    @Test
    @DisplayName("update 시 status를 DELETED로 변경 불가")
    void update_statusToDeleted_throwsException() {
        Post post = Post.builder()
                .title("title").slug("title").content("content").status(PostStatus.DRAFT).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(1L, new UpdatePostRequest("new", "new", PostStatus.DELETED)))
                .isInstanceOf(BlogException.class)
                .satisfies(e -> assertThat(((BlogException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    @DisplayName("DELETED 게시글은 update 불가")
    void update_deletedPost_throwsException() {
        Post post = Post.builder()
                .title("title").slug("title").content("content").status(PostStatus.DELETED).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(1L, new UpdatePostRequest("new", "new", PostStatus.DRAFT)))
                .isInstanceOf(BlogException.class)
                .satisfies(e -> assertThat(((BlogException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }
}
