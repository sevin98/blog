package sevin.dev.blog.domain.auth.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import sevin.dev.blog.domain.auth.AuthConstants;

import java.time.Duration;

@Component
public class CookieProvider {

    private final boolean secure;
    private final long expirationDays;
    private final String sameSite;

    public CookieProvider(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.jwt.expiration-days}") long expirationDays,
            @Value("${app.cookie.same-site}") String sameSite
    ) {
        this.secure = secure;
        this.expirationDays = expirationDays;
        this.sameSite = sameSite;
    }

    public ResponseCookie createTokenCookie(String token) {
        return buildCookie(token, Duration.ofDays(expirationDays));
    }

    public ResponseCookie createExpiredCookie() {
        return buildCookie("", Duration.ZERO);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(AuthConstants.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }
}
