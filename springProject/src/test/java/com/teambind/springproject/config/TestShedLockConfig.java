package com.teambind.springproject.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 테스트용 ShedLock 설정.
 *
 * 테스트 환경에서는 실제 분산 락이 필요 없으므로
 * Mockito를 사용한 Mock LockProvider를 제공한다.
 */
@TestConfiguration
public class TestShedLockConfig {

    @Bean
    @Primary
    public LockProvider lockProvider() {
        // Mock LockProvider 생성
        LockProvider mockProvider = mock(LockProvider.class);
        SimpleLock mockLock = mock(SimpleLock.class);

        // 항상 락 획득에 성공하도록 설정
        when(mockProvider.lock(any(LockConfiguration.class)))
                .thenReturn(Optional.of(mockLock));

        return mockProvider;
    }
}