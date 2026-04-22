package sevin.dev.blog.domain.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sevin.dev.blog.common.exception.BlogException;
import sevin.dev.blog.common.exception.ErrorCode;
import sevin.dev.blog.config.SecurityConfig;
import sevin.dev.blog.domain.auth.AuthConstants;
import sevin.dev.blog.domain.auth.jwt.CookieProvider;
import sevin.dev.blog.domain.auth.jwt.JwtProvider;
import sevin.dev.blog.domain.auth.oauth2.CustomOAuth2UserService;
import sevin.dev.blog.domain.auth.oauth2.OAuth2FailureHandler;
import sevin.dev.blog.domain.auth.oauth2.OAuth2SuccessHandler;
import sevin.dev.blog.domain.post.dto.CreatePostRequest;
import sevin.dev.blog.domain.post.dto.PostResponse;
import sevin.dev.blog.domain.post.dto.UpdatePostRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PostController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "app.cookie.secure=false",
        "app.cookie.same-site=Lax",
        "app.jwt.expiration-days=7",
        "app.admin.github-username=sevin98"
})
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.admin.github-username}")
    private String adminUsername;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CookieProvider cookieProvider;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                adminUsername, null, List.of(new SimpleGrantedAuthority(AuthConstants.ROLE_ADMIN)));
    }

    private PostResponse sampleResponse() {
        return new PostResponse(1L, UUID.randomUUID(), "Hello World", "hello-world",
                "content", PostStatus.DRAFT,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /admin/posts - 게시글 생성 201 반환")
    void create_returnsCreated() throws Exception {
        when(postService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/admin/posts")
                        .with(csrf())
                        .with(authentication(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePostRequest("Hello World", "content"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("hello-world"));
    }

    @Test
    @DisplayName("POST /admin/posts - 미인증 시 401 반환")
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePostRequest("title", "content"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/posts - title 공백이면 400 반환")
    void create_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/admin/posts")
                        .with(csrf())
                        .with(authentication(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /admin/posts - 목록 조회 200 반환")
    void findAll_returns200() throws Exception {
        when(postService.findAll(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/admin/posts")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /admin/posts - 미인증 시 401 반환")
    void findAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/posts/{slug} - 단건 조회 200 반환")
    void findBySlug_returns200() throws Exception {
        when(postService.findBySlug("hello-world")).thenReturn(sampleResponse());

        mockMvc.perform(get("/admin/posts/hello-world")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("hello-world"));
    }

    @Test
    @DisplayName("GET /admin/posts/{slug} - 미인증 시 401 반환")
    void findBySlug_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/posts/hello-world"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/posts/{slug} - 존재하지 않으면 404 반환")
    void findBySlug_notFound_returns404() throws Exception {
        when(postService.findBySlug("not-exist")).thenThrow(new BlogException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/admin/posts/not-exist")
                        .with(authentication(adminAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /admin/posts/{slug} - 수정 200 반환")
    void update_returns200() throws Exception {
        PostResponse updated = new PostResponse(1L, UUID.randomUUID(), "new title", "hello-world",
                "new content", PostStatus.DRAFT,
                LocalDateTime.now(), LocalDateTime.now());
        when(postService.update(eq("hello-world"), any())).thenReturn(updated);

        mockMvc.perform(patch("/admin/posts/hello-world")
                        .with(csrf())
                        .with(authentication(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePostRequest("new title", "new content"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("new title"));
    }

    @Test
    @DisplayName("PATCH /admin/posts/{slug} - 미인증 시 401 반환")
    void update_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/admin/posts/hello-world")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePostRequest("title", "content"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /admin/posts/{slug}/publish - 발행 200 반환")
    void publish_returns200() throws Exception {
        PostResponse published = new PostResponse(1L, UUID.randomUUID(), "Hello World", "hello-world",
                "content", PostStatus.PUBLISHED,
                LocalDateTime.now(), LocalDateTime.now());
        when(postService.publish("hello-world")).thenReturn(published);

        mockMvc.perform(patch("/admin/posts/hello-world/publish")
                        .with(csrf())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("PATCH /admin/posts/{slug}/publish - 미인증 시 401 반환")
    void publish_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/admin/posts/hello-world/publish").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /admin/posts/{slug}/publish - DELETED 게시글은 422 반환")
    void publish_deletedPost_returns422() throws Exception {
        when(postService.publish("deleted-post"))
                .thenThrow(new BlogException(ErrorCode.INVALID_STATUS_TRANSITION));

        mockMvc.perform(patch("/admin/posts/deleted-post/publish")
                        .with(csrf())
                        .with(authentication(adminAuth())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("DELETE /admin/posts/{slug} - soft delete 200 반환")
    void delete_returns200() throws Exception {
        doNothing().when(postService).delete("hello-world");

        mockMvc.perform(delete("/admin/posts/hello-world")
                        .with(csrf())
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /admin/posts/{slug} - 미인증 시 401 반환")
    void delete_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/admin/posts/hello-world").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
