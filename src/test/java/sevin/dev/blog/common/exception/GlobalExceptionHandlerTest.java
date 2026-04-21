package sevin.dev.blog.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;
import sevin.dev.blog.common.response.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BlogException은 ErrorCode에 정의된 HTTP 상태코드로 응답한다")
    void handleBlogException_returnsErrorCodeStatus() {
        BlogException e = new BlogException(ErrorCode.UNAUTHORIZED);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBlogException(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        assertThat(response.getBody().error().message()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500 Internal Server Error를 반환한다")
    void handleException_returns500() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleException(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    @DisplayName("Spring MVC 예외는 원래 HTTP 상태코드를 보존하고 ApiResponse 포맷으로 응답한다")
    void handleExceptionInternal_preservesStatusCode() {
        HttpStatusCode status = HttpStatus.METHOD_NOT_ALLOWED;
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                ex, null, new HttpHeaders(), status, mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(body.success()).isFalse();
        assertThat(body.error().code()).isEqualTo("HTTP_405");
    }

    @Test
    @DisplayName("BlogException(FORBIDDEN)은 403을 반환한다")
    void handleBlogException_forbiddenCode_returns403() {
        BlogException e = new BlogException(ErrorCode.FORBIDDEN);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBlogException(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().code()).isEqualTo("FORBIDDEN");
    }
}
