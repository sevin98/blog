package sevin.dev.blog.domain.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import sevin.dev.blog.domain.auth.AuthConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT 쿠키 → SecurityContext에 인증 정보 설정")
    void validToken_setsAuthentication() throws Exception {
        request.setCookies(new Cookie(AuthConstants.COOKIE_NAME, "valid-token"));
        when(jwtProvider.isValid("valid-token")).thenReturn(true);
        when(jwtProvider.extractUsername("valid-token")).thenReturn("sevin98");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("sevin98");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 JWT → SecurityContext 미설정, 필터 체인 계속 진행")
    void invalidToken_noAuthentication() throws Exception {
        request.setCookies(new Cookie(AuthConstants.COOKIE_NAME, "bad-token"));
        when(jwtProvider.isValid("bad-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("쿠키 없음 → SecurityContext 미설정, 필터 체인 계속 진행")
    void noCookies_noAuthentication() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).isValid(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("다른 이름의 쿠키만 있는 경우 → SecurityContext 미설정")
    void otherCookieOnly_noAuthentication() throws Exception {
        request.setCookies(new Cookie("otherCookie", "some-value"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).isValid(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 JWT → ROLE_ADMIN 권한 부여")
    void validToken_grantsAdminRole() throws Exception {
        request.setCookies(new Cookie(AuthConstants.COOKIE_NAME, "valid-token"));
        when(jwtProvider.isValid("valid-token")).thenReturn(true);
        when(jwtProvider.extractUsername("valid-token")).thenReturn("sevin98");

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals(AuthConstants.ROLE_ADMIN));
    }
}
