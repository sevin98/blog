package sevin.dev.blog.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import sevin.dev.blog.domain.auth.AuthConstants;
import sevin.dev.blog.domain.auth.jwt.CookieProvider;
import sevin.dev.blog.domain.auth.jwt.JwtProvider;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final CookieProvider cookieProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String githubUsername = oAuth2User.getAttribute(AuthConstants.GITHUB_USERNAME_ATTR);

        if (githubUsername == null) {
            log.error("GitHub username attribute missing from OAuth2 principal");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String token = jwtProvider.generate(githubUsername);
        log.info("JWT issued for admin");

        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.createTokenCookie(token).toString());
        // JWT는 httpOnly 쿠키에 설정 완료 → 프론트엔드 대시보드로 리다이렉트
        // 프론트엔드에서 /admin/auth/me 호출로 사용자 정보 조회
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + AuthConstants.ADMIN_LOGIN_REDIRECT);
    }
}
