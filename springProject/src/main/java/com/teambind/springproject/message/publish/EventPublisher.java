package com.teambind.springproject.message.publish;


import com.teambind.springproject.message.dto.*;
import com.teambind.springproject.message.event.Event;
import com.teambind.springproject.message.outbox.service.OutboxService;
import com.teambind.springproject.room.event.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도메인 이벤트를 Transactional Outbox Pattern으로 발행합니다.
 * <p>
 * 처리 흐름:
 * 1. 도메인 이벤트를 Message DTO로 변환
 * 2. Outbox 테이블에 저장 (DB 트랜잭션과 함께)
 * 3. @PostPersist에서 OutboxSavedEvent 발행
 * 4. ImmediatePublisher가 즉시 Kafka 발행 시도
 * 5. 실패 시 Scheduler가 재시도
 * <p>
 * <b>새로운 이벤트 추가 시 체크리스트</b>
 * <ol>
 *   <li>{@link #convertToMessage(Event)}에 case 추가</li>
 *   <li>{@link #getAggregateInfo(Event)}에 case 추가</li>
 *   <li>해당 이벤트의 MessageDTO에 from() 메서드 구현</li>
 *   <li>EventPublisherTest에 단위 테스트 추가</li>
 *   <li>EventConsumer에 MESSAGE_TYPE_MAP 및 convertToEvent() 업데이트</li>
 * </ol>
 *
 * @see EventPublisherTest#모든_도메인_이벤트가_메시지_변환을_지원해야_함()
 */
@Service
@RequiredArgsConstructor
public class EventPublisher {
	private final OutboxService outboxService;

	@Transactional
	public void publish(Event event) {
		// Event를 Message DTO로 변환 (ID: Long → String)
		Object messageDto = convertToMessage(event);

		// Aggregate 정보 추출
		AggregateInfo aggregateInfo = getAggregateInfo(event);

		// Outbox에 저장 (현재 트랜잭션 내에서)
		outboxService.saveToOutbox(
				event,
				messageDto,
				aggregateInfo.type(),
				aggregateInfo.id()
		);
	}

	/**
	 * Event를 Message DTO로 변환한다.
	 * <p>
	 * 모든 ID 필드가 Long → String으로 변환된다.
	 * <p>
	 * instanceof 체인을 사용하여 이벤트 타입별로 적절한 Message DTO로 변환합니다.
	 * 새로운 이벤트 타입 추가 시 if-else 블록을 추가해야 합니다.
	 *
	 * @param event 변환할 도메인 이벤트
	 * @return Kafka 메시지로 발행할 Message DTO
	 * @throws IllegalArgumentException 지원하지 않는 이벤트 타입인 경우
	 */
	private Object convertToMessage(Event event) {
		// SlotReservedEvent
		if (event instanceof SlotReservedEvent e) {
			return SlotReservedEventMessage.from(e);
		}

		// SlotCancelledEvent
		if (event instanceof SlotCancelledEvent e) {
			return SlotCancelledEventMessage.from(e);
		}

		// SlotRestoredEvent
		if (event instanceof SlotRestoredEvent e) {
			return SlotRestoredEventMessage.from(e);
		}

		// SlotGenerationRequestedEvent
		if (event instanceof SlotGenerationRequestedEvent e) {
			return SlotGenerationRequestedEventMessage.from(e);
		}

		// ClosedDateUpdateRequestedEvent
		if (event instanceof ClosedDateUpdateRequestedEvent e) {
			return ClosedDateUpdateRequestedEventMessage.from(e);
		}

		// Unknown event type
		throw new IllegalArgumentException(
				"지원하지 않는 이벤트 타입입니다: " + event.getClass().getName()
		);
	}

	/**
	 * Aggregate 정보를 추출합니다.
	 * <p>
	 * Aggregate Type과 ID는 Kafka 파티셔닝 및 추적에 사용됩니다.
	 *
	 * @param event 도메인 이벤트
	 * @return Aggregate 정보 (type, id)
	 */
	private AggregateInfo getAggregateInfo(Event event) {
		// SlotReservedEvent
		if (event instanceof SlotReservedEvent e) {
			return new AggregateInfo("RoomTimeSlot", String.valueOf(e.getRoomId()));
		}

		// SlotCancelledEvent
		if (event instanceof SlotCancelledEvent e) {
			return new AggregateInfo("Reservation", String.valueOf(e.getReservationId()));
		}

		// SlotRestoredEvent
		if (event instanceof SlotRestoredEvent e) {
			return new AggregateInfo("Reservation", e.getReservationId());
		}

		// SlotGenerationRequestedEvent
		if (event instanceof SlotGenerationRequestedEvent e) {
			return new AggregateInfo("Room", e.getRequestId());
		}

		// ClosedDateUpdateRequestedEvent
		if (event instanceof ClosedDateUpdateRequestedEvent e) {
			return new AggregateInfo("Room", e.getRequestId());
		}

		// Unknown event type
		throw new IllegalArgumentException(
				"지원하지 않는 이벤트 타입입니다: " + event.getClass().getName()
		);
	}

	/**
	 * Aggregate 정보를 담는 Record.
	 *
	 * @param type Aggregate 타입
	 * @param id   Aggregate ID
	 */
	private record AggregateInfo(String type, String id) {
	}
}
