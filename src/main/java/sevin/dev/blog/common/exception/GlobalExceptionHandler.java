package sevin.dev.blog.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import sevin.dev.blog.common.response.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlogException(BlogException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BlogException: {}", e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * Spring MVC 예외 (@Valid 등) — 기본 상태코드 보존하면서 ApiResponse 포맷 통일
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message));
    }

    /**
     * 그 외 Spring MVC 예외 (405, 415, 400 등) — 상태코드 그대로 유지, ApiResponse 포맷 적용
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        log.warn("Spring MVC exception [{}]: {}", statusCode, ex.getMessage());
        String code = "HTTP_" + statusCode.value();
        String message = ex.getMessage() != null ? ex.getMessage() : statusCode.toString();
        return ResponseEntity
                .status(statusCode)
                .headers(headers)
                .body(ApiResponse.error(code, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
