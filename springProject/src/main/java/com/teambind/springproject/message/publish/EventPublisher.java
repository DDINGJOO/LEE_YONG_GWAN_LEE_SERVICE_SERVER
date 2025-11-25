package com.teambind.springproject.message.publish;


import com.teambind.springproject.common.util.json.JsonUtil;
import com.teambind.springproject.message.dto.*;
import com.teambind.springproject.message.event.Event;
import com.teambind.springproject.room.event.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 도메인 이벤트를 Kafka 메시지로 발행합니다.
 * <p>
 * <b>새로운 이벤트 추가 시 체크리스트</b>
 * <ol>
 *   <li>{@link #convertToMessage(Event)}에 case 추가</li>
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
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final JsonUtil jsonUtil;

	public void publish(Event event) {
		// Event를 Message DTO로 변환 (ID: Long → String)
		Object messageDto = convertToMessage(event);

		String json = jsonUtil.toJson(messageDto);
		kafkaTemplate.send(event.getTopic(), json);
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
}
