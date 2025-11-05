package com.teambind.springproject.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Redis 설정.
 *
 * <p>ShedLockConfig가 요구하는 RedisConnectionFactory를 Mock으로 제공한다.
 */
@TestConfiguration
public class TestRedisConfig {
	
	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		return mock(RedisConnectionFactory.class);
	}
}
