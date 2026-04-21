package sevin.dev.blog.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * SessionCreationPolicy.STATELESS 환경에서 OAuth2 authorization request(state 포함)를
 * HttpSession 대신 HttpOnly 쿠키에 저장한다.
 * <p>
 * Spring Security 기본 구현(HttpSessionOAuth2AuthorizationRequestRepository)은
 * 세션이 없으면 authorization_request_not_found 에러를 발생시킨다.
 */
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookieValue(request, COOKIE_NAME)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, COOKIE_NAME);
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, serialize(authorizationRequest));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response, COOKIE_NAME);
        return authRequest;
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(request);
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("OAuth2AuthorizationRequest 직렬화 실패", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (OAuth2AuthorizationRequest) ois.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .forEach(c -> {
                    c.setValue("");
                    c.setPath("/");
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }
}
