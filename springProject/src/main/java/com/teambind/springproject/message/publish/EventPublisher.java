package com.teambind.springproject.message.publish;


import com.teambind.springproject.common.util.json.JsonUtil;
import com.teambind.springproject.message.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final JsonUtil jsonUtil;
	
	public void publish(Event event) {
		String json = jsonUtil.toJson(event);
		kafkaTemplate.send(event.getTopic(), json);
	}
}
