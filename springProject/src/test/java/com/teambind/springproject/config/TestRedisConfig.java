package com.teambind.springproject.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Redis 설정.
 * <p>
 * Redis 관련 빈들을 Mock으로 제공한다.
 * ShedLock은 TestShedLockConfig에서 H2 기반으로 처리한다.
 */
@TestConfiguration
public class TestRedisConfig {
	
	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		// Mock RedisConnectionFactory 제공
		// 실제 연결이 필요한 경우 TestContainers 사용 권장
		return mock(RedisConnectionFactory.class);
	}
	
	@Bean
	@Primary
	public StringRedisTemplate stringRedisTemplate() {
		// Mock StringRedisTemplate 제공
		return mock(StringRedisTemplate.class);
	}
}
