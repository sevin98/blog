package sevin.dev.blog.notion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevNoteScheduler {

	private final NotionApiService notionApiService;

	@Value("${notion.dev-note.enabled:true}")
	private boolean enabled;

	/**
	 * 매일 자동으로 dev note를 Notion에 기록합니다.
	 * 기본 스케줄: 매일 23시 (application.yml에서 설정 가능)
	 */
	@Scheduled(cron = "${notion.dev-note.schedule:0 0 23 * * *}")
	public void createDailyDevNote() {
		if (!enabled) {
			log.debug("Dev note scheduler is disabled");
			return;
		}

		log.info("Starting daily dev note creation...");

		notionApiService.checkTodayDevNoteExists()
				.flatMap(exists -> {
					if (exists) {
						log.info("Today's dev note already exists. Skipping creation.");
						return Mono.empty();
					}

					String title = "Dev Note - " + java.time.LocalDate.now().format(
							java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
					String content = "오늘의 개발 노트\n\n- 작업 내용: \n- 학습 내용: \n- 다음 할 일: ";

					return notionApiService.createDevNote(title, content)
							.doOnSuccess(pageId -> log.info("Daily dev note created successfully. Page ID: {}", pageId))
							.doOnError(error -> log.error("Failed to create daily dev note", error));
				})
				.subscribe();
	}
}
