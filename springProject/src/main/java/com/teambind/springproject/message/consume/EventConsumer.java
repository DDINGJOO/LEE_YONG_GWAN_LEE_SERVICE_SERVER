package com.teambind.springproject.message.consume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.message.dto.*;
import com.teambind.springproject.message.event.Event;
import com.teambind.springproject.message.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 통합 이벤트 컨슈머
 * <p>
 * 모든 이벤트 토픽을 구독하여 eventType에 따라 적절한 핸들러로 라우팅한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

	private static final Map<String, Class<?>> MESSAGE_TYPE_MAP = new HashMap<>();

	static {
		MESSAGE_TYPE_MAP.put("SlotReserved", SlotReservedEventMessage.class);
		MESSAGE_TYPE_MAP.put("SlotConfirmed", SlotConfirmedEventMessage.class);
		MESSAGE_TYPE_MAP.put("SlotCancelled", SlotCancelledEventMessage.class);
		MESSAGE_TYPE_MAP.put("SlotRestored", SlotRestoredEventMessage.class);
		MESSAGE_TYPE_MAP.put("RefundCompleted", RefundCompletedEventMessage.class);
		MESSAGE_TYPE_MAP.put("SlotGenerationRequested", SlotGenerationRequestedEventMessage.class);
		MESSAGE_TYPE_MAP.put("ClosedDateUpdateRequested", ClosedDateUpdateRequestedEventMessage.class);
	}

	private final List<EventHandler<? extends Event>> eventHandlers;
	private final ObjectMapper objectMapper;

	/**
	 * Kafka에서 이벤트를 수신하여 처리한다.
	 *
	 * @param message JSON 형식의 이벤트 메시지
	 */
	@KafkaListener(
			topics = {"reservation-reserved", "reservation-confirmed", "reservation-cancelled", "reservation-restored", "refund-completed", "slot-generation-requested", "closed-date-update-requested"},
			groupId = "room-operation-consumer-group"
	)
	public void consume(String message) {
		try {
			// 1. JSON에서 eventType 추출
			JsonNode jsonNode = objectMapper.readTree(message);
			String eventType = jsonNode.get("eventType").asText();

			log.info("Received event: type={}", eventType);

			// 2. eventType에 해당하는 Message DTO 클래스 찾기
			Class<?> messageClass = MESSAGE_TYPE_MAP.get(eventType);
			if (messageClass == null) {
				log.error("Unknown event type: {}", eventType);
				return;
			}

			// 3. JSON을 Message DTO로 역직렬화 (ID: String)
			Object messageDto = objectMapper.readValue(message, messageClass);

			// 4. Message DTO를 Event로 변환 (ID: String → Long)
			Event event = convertToEvent(messageDto);

			// 5. 해당 이벤트를 처리할 핸들러 찾기 (변환된 이벤트 타입 기준)
			EventHandler handler = findHandler(event.getEventTypeName());
			if (handler == null) {
				log.warn("No handler found for event type: {}", eventType);
				return;
			}

			// 6. 핸들러로 이벤트 처리 위임
			handler.handle(event);

			log.info("Event processed successfully: type={}", eventType);

		} catch (Exception e) {
			log.error("Failed to process event: {}", message, e);
			// TODO: 에러 처리 전략 (DLQ 전송, 재시도 등)
		}
	}

	/**
	 * Message DTO를 Event로 변환한다.
	 * 모든 ID 필드가 String → Long으로 변환된다.
	 */
	private Event convertToEvent(Object messageDto) {
		if (messageDto instanceof SlotReservedEventMessage) {
			return ((SlotReservedEventMessage) messageDto).toEvent();
		} else if (messageDto instanceof SlotConfirmedEventMessage) {
			return ((SlotConfirmedEventMessage) messageDto).toEvent();
		} else if (messageDto instanceof SlotCancelledEventMessage) {
			return ((SlotCancelledEventMessage) messageDto).toEvent();
		} else if (messageDto instanceof SlotRestoredEventMessage) {
			return ((SlotRestoredEventMessage) messageDto).toEvent();
		} else if (messageDto instanceof RefundCompletedEventMessage) {
			return ((RefundCompletedEventMessage) messageDto).toEvent();
		} else if (messageDto instanceof SlotGenerationRequestedEventMessage) {
			return ((SlotGenerationRequestedEventMessage) messageDto).toEvent();
		} else if (messageDto instanceof ClosedDateUpdateRequestedEventMessage) {
			return ((ClosedDateUpdateRequestedEventMessage) messageDto).toEvent();
		}

		throw new IllegalArgumentException("Unknown message type: " + messageDto.getClass().getName());
	}

	/**
	 * 이벤트 타입에 맞는 핸들러를 찾는다.
	 *
	 * @param eventType 이벤트 타입
	 * @return 해당 이벤트를 처리할 핸들러
	 */
	@SuppressWarnings("unchecked")
	private EventHandler findHandler(String eventType) {
		return eventHandlers.stream()
				.filter(handler -> handler.getSupportedEventType().equals(eventType))
				.findFirst()
				.orElse(null);
	}
}
