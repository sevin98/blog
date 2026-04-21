package sevin.dev.blog.domain.auth;

public final class AuthConstants {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String COOKIE_NAME = "accessToken";
    public static final String GITHUB_USERNAME_ATTR = "login";
    public static final String ADMIN_REDIRECT_PATH = "/admin/auth/me";

    private AuthConstants() {
        throw new UnsupportedOperationException("상수 클래스는 인스턴스화할 수 없습니다.");
    }
}
