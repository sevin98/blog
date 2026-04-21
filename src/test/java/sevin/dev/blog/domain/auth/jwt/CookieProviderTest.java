package sevin.dev.blog.domain.auth.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import sevin.dev.blog.domain.auth.AuthConstants;

import static org.assertj.core.api.Assertions.assertThat;

class CookieProviderTest {

    private final CookieProvider cookieProvider = new CookieProvider(false, 7);

    @Test
    @DisplayName("토큰 쿠키 생성 - HttpOnly, 7일 만료")
    void createTokenCookie() {
        ResponseCookie cookie = cookieProvider.createTokenCookie("test-token");

        assertThat(cookie.getName()).isEqualTo(AuthConstants.COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("test-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge().toDays()).isEqualTo(7);
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    @DisplayName("만료 쿠키 생성 - 빈 값, maxAge 0")
    void createExpiredCookie() {
        ResponseCookie cookie = cookieProvider.createExpiredCookie();

        assertThat(cookie.getName()).isEqualTo(AuthConstants.COOKIE_NAME);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().isZero()).isTrue();
    }

    @Test
    @DisplayName("secure=true 설정 시 Secure 속성 포함")
    void createTokenCookie_secure() {
        CookieProvider secureCookieProvider = new CookieProvider(true, 7);
        ResponseCookie cookie = secureCookieProvider.createTokenCookie("token");

        assertThat(cookie.isSecure()).isTrue();
    }
}
