package sevin.dev.blog.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private static final String DB_PING_SQL = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        HealthStatus dbStatus = checkDb();
        HealthStatus appStatus = dbStatus == HealthStatus.UP ? HealthStatus.UP : HealthStatus.DOWN;

        HttpStatus httpStatus = appStatus == HealthStatus.UP ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(new HealthResponse(appStatus, dbStatus));
    }

    private HealthStatus checkDb() {
        try {
            jdbcTemplate.queryForObject(DB_PING_SQL, Integer.class);
            return HealthStatus.UP;
        } catch (Exception e) {
            return HealthStatus.DOWN;
        }
    }

    enum HealthStatus { UP, DOWN }

    record HealthResponse(HealthStatus status, HealthStatus db) {}
}
