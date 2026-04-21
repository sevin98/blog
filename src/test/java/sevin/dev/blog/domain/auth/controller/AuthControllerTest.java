package sevin.dev.blog.domain.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sevin.dev.blog.config.SecurityConfig;
import sevin.dev.blog.domain.auth.AuthConstants;
import sevin.dev.blog.domain.auth.jwt.CookieProvider;
import sevin.dev.blog.domain.auth.jwt.JwtProvider;
import sevin.dev.blog.domain.auth.oauth2.CustomOAuth2UserService;
import sevin.dev.blog.domain.auth.oauth2.OAuth2FailureHandler;
import sevin.dev.blog.domain.auth.oauth2.OAuth2SuccessHandler;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "app.cookie.secure=false",
        "app.jwt.expiration-days=7",
        "app.admin.github-username=sevin98"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.admin.github-username}")
    private String adminUsername;

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

    @BeforeEach
    void setUp() {
        Mockito.when(cookieProvider.createExpiredCookie())
                .thenReturn(ResponseCookie.from(AuthConstants.COOKIE_NAME, "").maxAge(0).build());
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                adminUsername, null, List.of(new SimpleGrantedAuthority(AuthConstants.ROLE_ADMIN)));
    }

    @Test
    @DisplayName("GET /admin/auth/github - GitHub OAuth 리다이렉트")
    void githubLogin_redirects() throws Exception {
        mockMvc.perform(get("/admin/auth/github"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /admin/auth/me - 인증된 사용자 정보 반환")
    void me_authenticated() throws Exception {
        mockMvc.perform(get("/admin/auth/me").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.githubUsername").value(adminUsername));
    }

    @Test
    @DisplayName("GET /admin/auth/me - 미인증 시 401")
    void me_unauthorized() throws Exception {
        mockMvc.perform(get("/admin/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /admin/auth/logout - 로그아웃 성공")
    void logout_success() throws Exception {
        mockMvc.perform(post("/admin/auth/logout").with(csrf()).with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
