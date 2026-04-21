package sevin.dev.blog.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sevin.dev.blog.common.response.ApiResponse;
import sevin.dev.blog.domain.auth.AuthConstants;
import sevin.dev.blog.domain.auth.dto.MeResponse;
import sevin.dev.blog.domain.auth.jwt.CookieProvider;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CookieProvider cookieProvider;

    @GetMapping("/github")
    public void githubLogin(HttpServletResponse response) throws Exception {
        response.sendRedirect("/blog/oauth2/authorization/github");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.createExpiredCookie().toString());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal String githubUsername) {
        return ResponseEntity.ok(ApiResponse.ok(new MeResponse(githubUsername)));
    }
}
