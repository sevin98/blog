package sevin.dev.blog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sevin.dev.blog.domain.auth.jwt.JwtAuthenticationFilter;
import sevin.dev.blog.domain.auth.jwt.JwtProvider;
import sevin.dev.blog.domain.auth.oauth2.CustomOAuth2UserService;
import sevin.dev.blog.domain.auth.oauth2.OAuth2FailureHandler;
import sevin.dev.blog.domain.auth.oauth2.OAuth2SuccessHandler;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // XSRF-TOKEN 쿠키 기반 CSRF 방어 — JS 클라이언트가 읽어 X-XSRF-TOKEN 헤더로 전송
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/auth/github").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        // STATELESS 세션 정책 하에서 OAuth2 state를 쿠키에 저장
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(new HttpCookieOAuth2AuthorizationRequestRepository())
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "인증이 필요합니다."))
                        .accessDeniedHandler((request, response, e) ->
                                response.sendError(HttpStatus.FORBIDDEN.value(), "권한이 없습니다."))
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
