package sevin.dev.blog.domain.auth.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2FailureHandlerTest {

    @InjectMocks
    private OAuth2FailureHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("OAuth2 로그인 실패 → 에러 쿼리파라미터와 함께 프론트엔드로 리다이렉트")
    void onAuthenticationFailure_redirectsWithError() throws Exception {
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("access_denied");

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000/admin/login?error=unauthorized");
    }

    @Test
    @DisplayName("다른 frontendUrl 설정 시 해당 URL로 리다이렉트")
    void onAuthenticationFailure_usesConfiguredFrontendUrl() throws Exception {
        ReflectionTestUtils.setField(handler, "frontendUrl", "https://blog.example.com");
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("error");

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://blog.example.com/admin/login?error=unauthorized");
    }
}
