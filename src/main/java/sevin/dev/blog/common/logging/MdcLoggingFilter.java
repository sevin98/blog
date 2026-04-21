package sevin.dev.blog.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put(MdcKey.REQUEST_ID.getKey(), UUID.randomUUID().toString().substring(0, 8));
            MDC.put(MdcKey.METHOD.getKey(), request.getMethod());
            MDC.put(MdcKey.PATH.getKey(), request.getRequestURI());

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
