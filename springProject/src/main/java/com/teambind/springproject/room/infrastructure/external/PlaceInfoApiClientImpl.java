package com.teambind.springproject.room.infrastructure.external;

import com.teambind.springproject.room.command.domain.service.PlaceInfoApiClient;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Place Info Service API 클라이언트 구현체.
 * <p>
 * Hexagonal Architecture의 Adapter 역할:
 * <p>
 * - Domain Layer의 PlaceInfoApiClient Port 구현
 * - 외부 API 통신 담당 (Infrastructure Layer)
 * <p>
 * 현재 구현:
 * <p>
 * - 임시로 설정값 기반 SlotUnit 반환
 * - 추후 실제 API 호출로 대체 가능 (도메인 코드 수정 불필요)
 */
@Slf4j
@Component
public class PlaceInfoApiClientImpl implements PlaceInfoApiClient {

	/**
	 * 기본 SlotUnit 설정값.
	 * <p>
	 * application.yaml에서 설정 가능:
	 * room:
	 *   timeSlot:
	 *     defaultSlotUnit: HOUR  # 또는 HALF_HOUR, QUARTER_HOUR
	 */
	@Value("${room.timeSlot.defaultSlotUnit:HOUR}")
	private SlotUnit defaultSlotUnit;

	/**
	 * Place Info Service API URL (추후 실제 API 연동 시 사용).
	 */
	@Value("${place.info.api.url:http://localhost:8081}")
	private String placeInfoApiUrl;

	/**
	 * API 호출 타임아웃 (ms).
	 */
	@Value("${place.info.api.timeout:3000}")
	private int apiTimeout;

	@Override
	public SlotUnit getSlotUnit(Long roomId) {
		log.debug("Fetching SlotUnit for roomId={}", roomId);

		// TODO: 실제 Place Info Service API 호출
		// 현재는 설정값 반환 (임시 구현)

		/*
		 * 향후 실제 API 호출 예시:
		 *
		 * try {
		 *     RestTemplate restTemplate = new RestTemplate();
		 *     ResponseEntity<PlaceInfoResponse> response = restTemplate.getForEntity(
		 *         placeInfoApiUrl + "/api/places/" + roomId + "/slot-unit",
		 *         PlaceInfoResponse.class
		 *     );
		 *     return response.getBody().getSlotUnit();
		 * } catch (Exception e) {
		 *     log.warn("Failed to fetch SlotUnit from Place Info Service: roomId={}, error={}",
		 *              roomId, e.getMessage());
		 *     return defaultSlotUnit; // Fallback
		 * }
		 */

		log.debug("Returning default SlotUnit={} for roomId={}", defaultSlotUnit, roomId);
		return defaultSlotUnit;
	}

	@Override
	public boolean isHealthy() {
		// TODO: 실제 Health Check API 호출
		// 현재는 항상 true 반환 (임시 구현)

		/*
		 * 향후 실제 Health Check 예시:
		 *
		 * try {
		 *     RestTemplate restTemplate = new RestTemplate();
		 *     ResponseEntity<String> response = restTemplate.getForEntity(
		 *         placeInfoApiUrl + "/actuator/health",
		 *         String.class
		 *     );
		 *     return response.getStatusCode().is2xxSuccessful();
		 * } catch (Exception e) {
		 *     log.warn("Place Info Service health check failed: {}", e.getMessage());
		 *     return false;
		 * }
		 */

		log.debug("Place Info Service health check: OK (using default config)");
		return true;
	}
}