package com.teambind.springproject.room.service.impl;

import com.teambind.springproject.common.config.ExternalApiProperties;
import com.teambind.springproject.room.command.domain.service.PlaceInfoApiClientImpl;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlaceInfoApiClientImpl 테스트.
 *
 * 현재는 Mock 구현이므로 기본 동작만 검증한다.
 * 실제 API 연동 시 통합 테스트로 전환 필요.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceInfoApiClient 테스트")
class PlaceInfoApiClientImplTest {
	
	private ExternalApiProperties externalApiProperties;
	private PlaceInfoApiClientImpl apiClient;
	
	@BeforeEach
	void setUp() {
		// ExternalApiProperties와 PlaceInfoApi 설정
		externalApiProperties = new ExternalApiProperties();
		ExternalApiProperties.PlaceInfoApi config = new ExternalApiProperties.PlaceInfoApi();
		config.setUrl("http://localhost:8081");
		config.setConnectTimeout(5000);
		config.setReadTimeout(5000);
		externalApiProperties.setPlaceInfo(config);
		
		apiClient = new PlaceInfoApiClientImpl(externalApiProperties);
	}
	
	@Test
	@DisplayName("SlotUnit을 조회하면 기본값 HOUR를 반환한다 (Mock 구현)")
	void getSlotUnit_ReturnsMockDefault() {
		log.info("=== [SlotUnit을 조회하면 기본값 HOUR를 반환한다 (Mock 구현)] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long roomId = 100L;
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - Mock 구현: 모든 roomId에 대해 HOUR 반환");
		
		// When
		log.info("[When] apiClient.getSlotUnit() 호출");
		log.info("[When] - 파라미터: roomId={}", roomId);
		SlotUnit result = apiClient.getSlotUnit(roomId);
		log.info("[When] - 반환 값: {}", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 반환된 SlotUnit");
		log.info("[Then] - 예상(Expected): HOUR");
		log.info("[Then] - 실제(Actual): {}", result);
		assertThat(result).isEqualTo(SlotUnit.HOUR);
		log.info("[Then] - ✓ Mock 구현이 기본값 HOUR를 반환함");
		
		log.info("=== [SlotUnit을 조회하면 기본값 HOUR를 반환한다 (Mock 구현)] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("헬스체크는 항상 성공한다 (Mock 구현)")
	void isHealthy_AlwaysReturnsTrue() {
		log.info("=== [헬스체크는 항상 성공한다 (Mock 구현)] 테스트 시작 ===");
		
		// Given
		log.info("[Given] Mock 구현: 헬스체크는 항상 true 반환");
		
		// When
		log.info("[When] apiClient.isHealthy() 호출");
		boolean result = apiClient.isHealthy();
		log.info("[When] - 반환 값: {}", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 헬스체크 결과");
		log.info("[Then] - 예상(Expected): true (항상 성공)");
		log.info("[Then] - 실제(Actual): {}", result);
		assertThat(result).isTrue();
		log.info("[Then] - ✓ Mock 구현이 항상 true를 반환함");
		
		log.info("=== [헬스체크는 항상 성공한다 (Mock 구현)] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("다양한 roomId로 SlotUnit을 조회해도 동일한 기본값을 반환한다")
	void getSlotUnit_ConsistentDefaultForAllRoomIds() {
		log.info("=== [다양한 roomId로 SlotUnit을 조회해도 동일한 기본값을 반환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long[] roomIds = {100L, 200L, 300L, 999L};
		log.info("[Given] - 테스트할 roomIds: {}", (Object) roomIds);
		log.info("[Given] - Mock 구현: 모든 roomId에 대해 동일한 HOUR 반환");
		
		// When & Then
		log.info("[When & Then] 여러 roomId로 조회 및 검증");
		for (int i = 0; i < roomIds.length; i++) {
			Long roomId = roomIds[i];
			log.info("[When & Then] [{}번째 roomId] - roomId: {}", i + 1, roomId);
			SlotUnit result = apiClient.getSlotUnit(roomId);
			log.info("[When & Then] [{}번째 roomId] - 반환 값: {}", i + 1, result);
			log.info("[Then] - 예상(Expected): HOUR");
			log.info("[Then] - 실제(Actual): {}", result);
			assertThat(result).isEqualTo(SlotUnit.HOUR);
			log.info("[Then] - ✓ roomId={}에 대해 HOUR 반환 확인", roomId);
		}
		
		log.info("=== [다양한 roomId로 SlotUnit을 조회해도 동일한 기본값을 반환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("null roomId로 SlotUnit을 조회해도 기본값을 반환한다 (Mock 구현 특성)")
	void getSlotUnit_NullRoomId() {
		log.info("=== [null roomId로 SlotUnit을 조회해도 기본값을 반환한다 (Mock 구현 특성)] 테스트 시작 ===");
		
		// Given
		log.info("[Given] Mock 구현: 파라미터를 무시하고 기본값 HOUR 반환");
		
		// When
		log.info("[When] apiClient.getSlotUnit() 호출");
		log.info("[When] - 파라미터: roomId=null");
		SlotUnit result = apiClient.getSlotUnit(null);
		log.info("[When] - 반환 값: {}", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] null roomId로 조회 시 반환값");
		log.info("[Then] - 예상(Expected): HOUR (Mock 구현 특성)");
		log.info("[Then] - 실제(Actual): {}", result);
		assertThat(result).isEqualTo(SlotUnit.HOUR);
		log.info("[Then] - ✓ Mock 구현은 null roomId도 처리하고 기본값 반환");
		
		log.info("=== [null roomId로 SlotUnit을 조회해도 기본값을 반환한다 (Mock 구현 특성)] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("설정된 URL이 올바르게 로드된다")
	void configurationIsLoaded() {
		log.info("=== [설정된 URL이 올바르게 로드된다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] setUp()에서 설정된 ExternalApiProperties 사용");
		
		// When
		log.info("[When] externalApiProperties에서 설정값 조회");
		String url = externalApiProperties.getPlaceInfo().getUrl();
		int connectTimeout = externalApiProperties.getPlaceInfo().getConnectTimeout();
		int readTimeout = externalApiProperties.getPlaceInfo().getReadTimeout();
		log.info("[When] - url: {}", url);
		log.info("[When] - connectTimeout: {}ms", connectTimeout);
		log.info("[When] - readTimeout: {}ms", readTimeout);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] URL 설정");
		log.info("[Then] - 예상(Expected): http://localhost:8081");
		log.info("[Then] - 실제(Actual): {}", url);
		assertThat(url).isEqualTo("http://localhost:8081");
		log.info("[Then] - ✓ URL 설정 일치");
		
		log.info("[Then] [검증2] Connect Timeout 설정");
		log.info("[Then] - 예상(Expected): 5000ms");
		log.info("[Then] - 실제(Actual): {}ms", connectTimeout);
		assertThat(connectTimeout).isEqualTo(5000);
		log.info("[Then] - ✓ Connect Timeout 설정 일치");
		
		log.info("[Then] [검증3] Read Timeout 설정");
		log.info("[Then] - 예상(Expected): 5000ms");
		log.info("[Then] - 실제(Actual): {}ms", readTimeout);
		assertThat(readTimeout).isEqualTo(5000);
		log.info("[Then] - ✓ Read Timeout 설정 일치");
		
		log.info("=== [설정된 URL이 올바르게 로드된다] 테스트 성공 ===");
	}
}
