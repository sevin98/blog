package sevin.dev.blog.domain.auth;

public final class AuthConstants {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String COOKIE_NAME = "accessToken";
    public static final String GITHUB_USERNAME_ATTR = "login";

    /** OAuth2 로그인 성공 후 브라우저가 이동할 프론트엔드 라우트 */
    public static final String ADMIN_LOGIN_REDIRECT = "/admin";

    /** 인증된 사용자 정보를 반환하는 백엔드 API 엔드포인트 */
    public static final String ADMIN_ME_API = "/admin/auth/me";

    private AuthConstants() {
        throw new UnsupportedOperationException("상수 클래스는 인스턴스화할 수 없습니다.");
    }
}
