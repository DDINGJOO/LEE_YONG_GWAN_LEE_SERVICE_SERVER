package com.teambind.springproject.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 작업 설정.
 * <p>
 * @Async 어노테이션을 사용하는 비동기 메서드들을 위한 스레드풀을 설정합니다.
 * <p>
 * 설정된 Executor:
 * - outboxExecutor: Outbox 즉시 발행 전용 (ImmediatePublisher)
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Outbox 즉시 발행을 위한 전용 스레드풀.
	 * <p>
	 * 설정 근거:
	 * - Core Size: 5 - 일반적인 동시 이벤트 발행량
	 * - Max Size: 20 - 부하 시 확장 가능
	 * - Queue Capacity: 100 - 스레드 대기열
	 * - Keep Alive: 60초 - 유휴 스레드 정리
	 * <p>
	 * CallerRunsPolicy:
	 * - 큐가 가득 찬 경우 호출 스레드에서 직접 실행
	 * - 메시지 손실 방지 (Outbox에는 이미 저장됨)
	 * - 자연스러운 백프레셔 (시스템 과부하 시)
	 *
	 * @return Outbox 전용 Executor
	 */
	@Bean(name = "outboxExecutor")
	public Executor outboxExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// 기본 스레드 수
		executor.setCorePoolSize(5);

		// 최대 스레드 수
		executor.setMaxPoolSize(20);

		// 큐 용량
		executor.setQueueCapacity(100);

		// 유휴 스레드 유지 시간 (초)
		executor.setKeepAliveSeconds(60);

		// 스레드 이름 접두사 (로그 추적용)
		executor.setThreadNamePrefix("outbox-async-");

		// 큐 초과 시 정책: 호출 스레드에서 실행
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		// 초기화 대기
		executor.setWaitForTasksToCompleteOnShutdown(true);

		// 종료 대기 시간 (초)
		executor.setAwaitTerminationSeconds(60);

		executor.initialize();

		log.info("Outbox async executor initialized: coreSize={}, maxSize={}, queueCapacity={}",
				executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

		return executor;
	}
}