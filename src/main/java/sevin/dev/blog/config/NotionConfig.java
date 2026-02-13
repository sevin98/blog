package sevin.dev.blog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NotionConfig {

	@Value("${notion.api.base-url}")
	private String notionBaseUrl;

	@Value("${notion.api.token}")
	private String notionToken;

	@Bean
	public WebClient notionWebClient() {
		return WebClient.builder()
				.baseUrl(notionBaseUrl)
				.defaultHeader("Authorization", "Bearer " + notionToken)
				.defaultHeader("Notion-Version", "2022-06-28")
				.defaultHeader("Content-Type", "application/json")
				.build();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
