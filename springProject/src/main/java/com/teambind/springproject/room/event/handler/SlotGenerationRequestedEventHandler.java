package com.teambind.springproject.room.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.room.entity.SlotGenerationRequest;
import com.teambind.springproject.room.event.event.SlotGenerationRequestedEvent;
import com.teambind.springproject.room.repository.SlotGenerationRequestRepository;
import com.teambind.springproject.room.command.domain.service.TimeSlotGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 슬롯 생성 요청 이벤트 핸들러.
 * <p>
 * 비동기로 슬롯을 생성하고 상태를 업데이트한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotGenerationRequestedEventHandler implements EventHandler<SlotGenerationRequestedEvent> {

	private final TimeSlotGenerationService generationService;
	private final SlotGenerationRequestRepository requestRepository;

	@Override
	@Transactional
	public void handle(SlotGenerationRequestedEvent event) {
		log.info("Processing SlotGenerationRequestedEvent: requestId={}, roomId={}",
				event.getRequestId(), event.getRoomId());

		// 1. 요청 조회
		SlotGenerationRequest request = requestRepository.findById(event.getRequestId())
				.orElseThrow(() -> new IllegalStateException(
						"SlotGenerationRequest not found: " + event.getRequestId()
				));

		try {
			// 2. 처리 시작 상태로 변경
			request.markAsInProgress();
			requestRepository.save(request);

			log.info("Starting slot generation: requestId={}", event.getRequestId());

			// 3. 슬롯 생성
			int totalSlots = generationService.generateSlotsForDateRange(
					event.getRoomId(),
					event.getStartDate(),
					event.getEndDate()
			);

			// 4. 완료 상태로 변경
			request.markAsCompleted(totalSlots);
			requestRepository.save(request);

			log.info("Slot generation completed: requestId={}, totalSlots={}",
					event.getRequestId(), totalSlots);

		} catch (Exception e) {
			// 5. 실패 상태로 변경
			log.error("Slot generation failed: requestId={}", event.getRequestId(), e);

			request.markAsFailed(e.getMessage());
			requestRepository.save(request);

			// 예외를 다시 던지지 않음 - DLQ로 이동하지 않고 상태만 업데이트
		}
	}

	@Override
	public String getSupportedEventType() {
		return "SlotGenerationRequested";
	}
}
