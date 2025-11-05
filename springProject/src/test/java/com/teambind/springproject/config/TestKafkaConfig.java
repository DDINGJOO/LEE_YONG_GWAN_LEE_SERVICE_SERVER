package com.teambind.springproject.config;

import com.teambind.springproject.common.util.json.JsonUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Kafka 설정.
 *
 * EventPublisher가 요구하는 KafkaTemplate과 JsonUtil을 Mock으로 제공한다.
 */
@TestConfiguration
public class TestKafkaConfig {

	@Bean
	@Primary
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return mock(KafkaTemplate.class);
	}

	@Bean
	@Primary
	public JsonUtil jsonUtil() {
		return mock(JsonUtil.class);
	}
}
