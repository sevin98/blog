package sevin.dev.blog.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        String dbStatus = checkDb();
        return ResponseEntity.ok(new HealthResponse("UP", dbStatus));
    }

    private String checkDb() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    record HealthResponse(String status, String db) {}
}
