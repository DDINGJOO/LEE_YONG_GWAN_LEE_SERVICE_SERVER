package com.teambind.springproject.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

/**
 * 테스트용 ShedLock 설정.
 *
 * 테스트 환경에서는 실제 분산 락이 필요없으므로 NoOp 구현을 제공한다.
 * 모든 락 요청에 대해 항상 성공을 반환하여 테스트가 정상적으로 실행되도록 한다.
 */
@TestConfiguration
public class TestShedLockConfig {

    @Bean
    @Primary
    public LockProvider lockProvider() {
        // 테스트용 NoOp LockProvider
        // 모든 락 요청에 대해 항상 성공을 반환
        return new LockProvider() {
            @Override
            public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
                // 항상 락 획득에 성공하는 SimpleLock 반환
                return Optional.of(new SimpleLock() {
                    @Override
                    public void unlock() {
                        // NoOp - 테스트에서는 실제 언락 동작 불필요
                    }
                });
            }
        };
    }
}