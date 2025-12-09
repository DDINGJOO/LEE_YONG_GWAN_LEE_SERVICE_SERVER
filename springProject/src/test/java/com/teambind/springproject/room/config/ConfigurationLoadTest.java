package com.teambind.springproject.room.config;

import com.teambind.springproject.room.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 테스트 환경 구성 값 로드 검증.
 * <p>
 * application-test.yaml의 설정이 올바르게 로드되는지 확인한다.
 */
@DisplayName("테스트 구성 값 로드 검증")
class ConfigurationLoadTest extends BaseIntegrationTest {
	
	@Value("${room.timeSlot.pending.expiration.minutes}")
	private int pendingExpirationMinutes;
	
	@Value("${room.timeSlot.rollingWindow.days}")
	private int rollingWindowDays;
	
	@Test
	@DisplayName("Room 모듈 구성 값이 올바르게 로드됨")
	void testRoomConfigurationLoaded() {
		// Then
		assertThat(pendingExpirationMinutes)
				.isEqualTo(30)
				.as("pending expiration minutes should be 30");
		
		assertThat(rollingWindowDays)
				.isEqualTo(30)
				.as("rolling window days should be 30");
	}
}
