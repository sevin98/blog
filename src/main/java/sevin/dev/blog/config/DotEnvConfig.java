package sevin.dev.blog.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DotEnvConfig {

	@Value("${spring.profiles.active:local}")
	private String activeProfile;

	@PostConstruct
	public void loadDotEnv() {
		// profile에 따라 .env.local 또는 .env.production 파일 로드
		String envFile = ".env." + activeProfile;
		
		log.info("Loading environment variables from: {}", envFile);
		
		Dotenv dotenv = Dotenv.configure()
				.filename(envFile)
				.ignoreIfMissing()
				.load();
		
		// .env 파일의 변수들을 시스템 환경변수로 등록
		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			String value = entry.getValue();
			// 이미 시스템 환경변수로 설정되어 있지 않은 경우에만 설정
			if (System.getenv(key) == null) {
				System.setProperty(key, value);
				log.debug("Loaded environment variable: {}", key);
			}
		});
		
		log.info("Environment variables loaded from {}", envFile);
	}
}
