package sevin.dev.blog.domain.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import sevin.dev.blog.common.logging.MdcKey;
import sevin.dev.blog.domain.auth.AuthConstants;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        extractToken(request).ifPresent(token -> {
            if (jwtProvider.isValid(token)) {
                String username = jwtProvider.extractUsername(token);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority(AuthConstants.ROLE_ADMIN))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                MDC.put(MdcKey.USERNAME.getKey(), username);
                log.debug("JWT authenticated");
            }
        });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> AuthConstants.COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
