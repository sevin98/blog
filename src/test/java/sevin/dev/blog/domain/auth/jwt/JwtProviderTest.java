package sevin.dev.blog.domain.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String TEST_USERNAME = "test-admin";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        // 64자 이상의 테스트용 시크릿 (운영에서는 env var 사용)
        String secret = "test-secret-key-must-be-at-least-64-characters-long-for-hmac-sha256-algorithm";
        jwtProvider = new JwtProvider(secret, 7);
    }

    @Test
    @DisplayName("토큰 생성 후 username 추출 성공")
    void generate_and_extract() {
        String token = jwtProvider.generate(TEST_USERNAME);
        assertThat(jwtProvider.extractUsername(token)).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void isValid_true() {
        String token = jwtProvider.generate(TEST_USERNAME);
        assertThat(jwtProvider.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰 검증 실패")
    void isValid_false_tampered() {
        String token = jwtProvider.generate(TEST_USERNAME) + "tampered";
        assertThat(jwtProvider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰 검증 실패")
    void isValid_false_empty() {
        assertThat(jwtProvider.isValid("")).isFalse();
    }
}
