package sevin.dev.blog.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotionApiService {

	private final WebClient notionWebClient;
	private final ObjectMapper objectMapper;

	@Value("${notion.api.database-id}")
	private String databaseId;

	/**
	 * Notion 데이터베이스에 새로운 페이지(dev note)를 생성합니다.
	 * 
	 * @param title 제목
	 * @param content 내용
	 * @return 생성된 페이지 ID
	 */
	public Mono<String> createDevNote(String title, String content) {
		ObjectNode requestBody = objectMapper.createObjectNode();
		
		// parent 설정 (데이터베이스 ID)
		ObjectNode parent = objectMapper.createObjectNode();
		parent.put("database_id", databaseId);
		requestBody.set("parent", parent);
		
		// properties 설정
		ObjectNode properties = objectMapper.createObjectNode();
		
		// 제목 속성
		ObjectNode titleProperty = objectMapper.createObjectNode();
		ObjectNode titleText = objectMapper.createObjectNode();
		titleText.put("content", title);
		titleProperty.set("rich_text", objectMapper.createArrayNode().add(titleText));
		properties.set("제목", titleProperty);
		
		// 날짜 속성 (오늘 날짜)
		ObjectNode dateProperty = objectMapper.createObjectNode();
		ObjectNode dateValue = objectMapper.createObjectNode();
		dateValue.put("start", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
		dateProperty.set("date", dateValue);
		properties.set("날짜", dateProperty);
		
		// 내용 속성
		ObjectNode contentProperty = objectMapper.createObjectNode();
		ObjectNode contentText = objectMapper.createObjectNode();
		contentText.put("content", content);
		contentProperty.set("rich_text", objectMapper.createArrayNode().add(contentText));
		properties.set("내용", contentProperty);
		
		requestBody.set("properties", properties);
		
		// children (내용을 블록으로 추가)
		ObjectNode paragraph = objectMapper.createObjectNode();
		paragraph.put("object", "block");
		paragraph.put("type", "paragraph");
		ObjectNode paragraphContent = objectMapper.createObjectNode();
		ObjectNode paragraphText = objectMapper.createObjectNode();
		paragraphText.put("type", "text");
		ObjectNode paragraphTextContent = objectMapper.createObjectNode();
		paragraphTextContent.put("content", content);
		paragraphText.set("text", paragraphTextContent);
		paragraphContent.set("rich_text", objectMapper.createArrayNode().add(paragraphText));
		paragraph.set("paragraph", paragraphContent);
		
		requestBody.set("children", objectMapper.createArrayNode().add(paragraph));

		return notionWebClient.post()
				.uri("/pages")
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.map(response -> {
					String pageId = response.get("id").asText();
					log.info("Notion dev note created successfully. Page ID: {}", pageId);
					return pageId;
				})
				.doOnError(error -> log.error("Failed to create Notion dev note", error));
	}

	/**
	 * 오늘 날짜의 dev note가 이미 존재하는지 확인합니다.
	 * 
	 * @return 존재 여부
	 */
	public Mono<Boolean> checkTodayDevNoteExists() {
		String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		
		ObjectNode requestBody = objectMapper.createObjectNode();
		ObjectNode filter = objectMapper.createObjectNode();
		ObjectNode dateFilter = objectMapper.createObjectNode();
		ObjectNode equals = objectMapper.createObjectNode();
		equals.put("equals", today);
		dateFilter.set("equals", equals);
		filter.set("날짜", dateFilter);
		requestBody.set("filter", filter);

		return notionWebClient.post()
				.uri("/databases/{databaseId}/query", databaseId)
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(JsonNode.class)
				.map(response -> {
					JsonNode results = response.get("results");
					boolean exists = results != null && results.isArray() && results.size() > 0;
					log.debug("Today's dev note exists: {}", exists);
					return exists;
				})
				.onErrorReturn(false);
	}
}
