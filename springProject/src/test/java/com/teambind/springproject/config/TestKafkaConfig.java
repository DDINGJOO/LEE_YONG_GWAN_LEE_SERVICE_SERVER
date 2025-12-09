package com.teambind.springproject.config;

import com.teambind.springproject.common.util.json.JsonUtil;
import com.teambind.springproject.common.util.json.JsonUtilWithObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Kafka 설정.
 * <p>
 * EventPublisher와 OutboxImmediatePublisher가 요구하는 KafkaTemplate과 JsonUtil을 제공한다.
 * - KafkaTemplate: Mock (실제 Kafka 발행 불필요)
 * - JsonUtil: 실제 구현 (OutboxMessage 생성 시 payload 직렬화 필요)
 */
@TestConfiguration
public class TestKafkaConfig {
	
	@Bean
	@Primary
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return mock(KafkaTemplate.class);
	}
	
	@Bean
	public KafkaTemplate<String, String> stringKafkaTemplate() {
		return mock(KafkaTemplate.class);
	}
	
	@Bean
	@Primary
	public JsonUtil jsonUtil() {
		return new JsonUtilWithObjectMapper();
	}
}
