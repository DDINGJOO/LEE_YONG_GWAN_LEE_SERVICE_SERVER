package com.teambind.springproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정.
 * <p>
 * @Scheduled 어노테이션을 사용하는 Scheduler들을 활성화합니다.
 * <p>
 * 활성화되는 Scheduler:
 * - OutboxScheduler: Outbox 메시지 발행 및 정리
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}