package com.teambind.springproject.room;

import com.teambind.springproject.config.TestKafkaConfig;
import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.config.TestShedLockConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Room 모듈 통합 테스트 기본 설정.
 *
 * 모든 Room 모듈 통합 테스트가 상속받는 베이스 클래스.
 * 공통 테스트 설정을 중앙화하여 관리한다.
 *
 * 제공되는 기능:
 * - H2 인메모리 데이터베이스
 * - Mock Kafka/Redis 설정
 * - H2 기반 ShedLock
 * - 트랜잭션 롤백
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "room.timeSlot.pending.expiration.minutes=30",
        "room.timeSlot.rollingWindow.days=30"
})
@Import({TestRedisConfig.class, TestKafkaConfig.class, TestShedLockConfig.class})
@Transactional
public abstract class BaseIntegrationTest {
    // 공통 유틸리티 메서드나 설정을 여기에 추가할 수 있음
}