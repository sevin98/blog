package sevin.dev.blog.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sevin.dev.blog.config.SecurityConfig;
import sevin.dev.blog.domain.auth.jwt.CookieProvider;
import sevin.dev.blog.domain.auth.jwt.JwtProvider;
import sevin.dev.blog.domain.auth.oauth2.CustomOAuth2UserService;
import sevin.dev.blog.domain.auth.oauth2.OAuth2FailureHandler;
import sevin.dev.blog.domain.auth.oauth2.OAuth2SuccessHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = HealthController.class,
        excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class
)
@Import(SecurityConfig.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "app.frontend-url=http://localhost:3000",
        "app.cookie.secure=false",
        "app.cookie.same-site=Lax",
        "app.jwt.expiration-days=7",
        "app.admin.github-username=sevin98"
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

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

    @Test
    @DisplayName("GET /health - DB 정상 시 200 UP 반환")
    void health_db_up() throws Exception {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class))).thenReturn(1);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.db").value("UP"));
    }

    @Test
    @DisplayName("GET /health - DB 장애 시 503 DOWN 반환")
    void health_db_down() throws Exception {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.db").value("DOWN"));
    }
}
