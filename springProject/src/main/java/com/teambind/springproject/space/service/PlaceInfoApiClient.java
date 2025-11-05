package com.teambind.springproject.space.service;

import com.teambind.springproject.space.entity.enums.SlotUnit;
import org.springframework.stereotype.Service;

/**
 * Place Info Service와 통신하는 API 클라이언트.
 *
 * 주요 책임:
 *
 *
 *   Room의 SlotUnit 조회
 *   외부 서비스 장애 대응 (Fallback, Circuit Breaker)
 *
 */
@Service
public interface PlaceInfoApiClient {
	
	/**
	 * Room ID로 SlotUnit을 조회한다.
	 *
	 * <p>API 호출 실패 시 기본값(HALF_HOUR)을 반환한다.
	 *
	 * @param roomId 룸 ID
	 * @return SlotUnit (HOUR 또는 HALF_HOUR)
	 */
	SlotUnit getSlotUnit(Long roomId);
	
	/**
	 * API 연결 상태를 확인한다.
	 *
	 * @return API가 정상이면 true, 아니면 false
	 */
	boolean isHealthy();
}
