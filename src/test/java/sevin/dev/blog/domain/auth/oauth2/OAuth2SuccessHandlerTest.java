package sevin.dev.blog.domain.auth.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import sevin.dev.blog.domain.auth.AuthConstants;
import sevin.dev.blog.domain.auth.jwt.CookieProvider;
import sevin.dev.blog.domain.auth.jwt.JwtProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CookieProvider cookieProvider;

    @InjectMocks
    private OAuth2SuccessHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    private OAuth2User oAuth2User;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");
        request = new MockHttpServletRequest();
        request.setContextPath("/blog");
        response = new MockHttpServletResponse();
        oAuth2User = mock(OAuth2User.class);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
    }

    @Test
    @DisplayName("GitHub username 존재 → JWT 발급 후 프론트엔드로 리다이렉트")
    void success_redirectsWithJwtCookie() throws Exception {
        when(oAuth2User.getAttribute(AuthConstants.GITHUB_USERNAME_ATTR)).thenReturn("sevin98");
        when(jwtProvider.generate("sevin98")).thenReturn("jwt-token");
        ResponseCookie cookie = ResponseCookie.from(AuthConstants.COOKIE_NAME, "jwt-token").build();
        when(cookieProvider.createTokenCookie("jwt-token")).thenReturn(cookie);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getHeader("Set-Cookie")).contains("jwt-token");
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:3000" + AuthConstants.ADMIN_LOGIN_REDIRECT);
    }

    @Test
    @DisplayName("GitHub username null → 500 에러 반환, 리다이렉트 없음")
    void missingUsername_returns500() throws Exception {
        when(oAuth2User.getAttribute(AuthConstants.GITHUB_USERNAME_ATTR)).thenReturn(null);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getRedirectedUrl()).isNull();
        verify(jwtProvider, never()).generate(any());
    }

    @Test
    @DisplayName("성공 시 JwtProvider.generate 정확한 username으로 호출")
    void success_callsGenerateWithCorrectUsername() throws Exception {
        when(oAuth2User.getAttribute(AuthConstants.GITHUB_USERNAME_ATTR)).thenReturn("sevin98");
        when(jwtProvider.generate("sevin98")).thenReturn("jwt-token");
        ResponseCookie cookie = ResponseCookie.from(AuthConstants.COOKIE_NAME, "jwt-token").build();
        when(cookieProvider.createTokenCookie("jwt-token")).thenReturn(cookie);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtProvider).generate("sevin98");
        verify(cookieProvider).createTokenCookie("jwt-token");
    }
}
