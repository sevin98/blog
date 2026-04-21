package sevin.dev.blog.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "local";
        String envFile = ".env." + profile;

        Dotenv dotenv = Dotenv.configure()
                .filename(envFile)
                .ignoreIfMissing()
                .load();

        Map<String, Object> properties = new HashMap<>();
        dotenv.entries().forEach(entry -> {
            // 시스템 환경변수가 없는 경우에만 .env 파일 값 사용
            if (System.getenv(entry.getKey()) == null) {
                properties.put(entry.getKey(), entry.getValue());
            }
        });

        if (!properties.isEmpty()) {
            // 가장 낮은 우선순위로 추가 (시스템 환경변수 > .env 파일)
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
