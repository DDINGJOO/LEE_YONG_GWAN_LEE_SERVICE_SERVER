package com.teambind.springproject.message.publish;


import com.teambind.springproject.common.util.json.JsonUtil;
import com.teambind.springproject.message.dto.*;
import com.teambind.springproject.message.event.Event;
import com.teambind.springproject.room.event.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
	 * 모든 ID 필드가 Long → String으로 변환된다.
	 */
	private Object convertToMessage(Event event) {
		if (event instanceof SlotReservedEvent) {
			return SlotReservedEventMessage.from((SlotReservedEvent) event);
		} else if (event instanceof SlotCancelledEvent) {
			return SlotCancelledEventMessage.from((SlotCancelledEvent) event);
		} else if (event instanceof SlotRestoredEvent) {
			return SlotRestoredEventMessage.from((SlotRestoredEvent) event);
		} else if (event instanceof SlotGenerationRequestedEvent) {
			return SlotGenerationRequestedEventMessage.from((SlotGenerationRequestedEvent) event);
		} else if (event instanceof ClosedDateUpdateRequestedEvent) {
			return ClosedDateUpdateRequestedEventMessage.from((ClosedDateUpdateRequestedEvent) event);
		}

		throw new IllegalArgumentException("Unknown event type: " + event.getClass().getName());
	}
}
