package com.teambind.springproject.space.service.impl;

import com.teambind.springproject.common.config.ExternalApiProperties;
import com.teambind.springproject.space.entity.enums.SlotUnit;
import com.teambind.springproject.space.service.PlaceInfoApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Place Info API 클라이언트 구현체.
 * <p>
 * 현재는 Mock 구현으로, 실제 API 연동 시 변경 필요.
 */
@Service
public class PlaceInfoApiClientImpl implements PlaceInfoApiClient {
	
	private static final Logger log = LoggerFactory.getLogger(PlaceInfoApiClientImpl.class);
	
	private final ExternalApiProperties externalApiProperties;
	
	public PlaceInfoApiClientImpl(ExternalApiProperties externalApiProperties) {
		this.externalApiProperties = externalApiProperties;
		log.info("PlaceInfoApiClient initialized with URL: {}",
				externalApiProperties.getPlaceInfo().getUrl());
	}
	
	@Override
	public SlotUnit getSlotUnit(Long roomId) {
		// TODO: 실제 Place Info Service API 호출 구현
		// String apiUrl = externalApiProperties.getPlaceInfo().getUrl();
		// RestTemplate 또는 WebClient를 사용하여 GET {apiUrl}/rooms/{roomId}/slot-unit 호출
		// 현재는 기본값 반환
		log.debug("Fetching SlotUnit for roomId={} (using default: HOUR)", roomId);
		return SlotUnit.HOUR;
	}
	
	@Override
	public boolean isHealthy() {
		// TODO: 실제 헬스체크 구현
		// String apiUrl = externalApiProperties.getPlaceInfo().getUrl();
		// RestTemplate 또는 WebClient를 사용하여 GET {apiUrl}/health 호출
		log.debug("Health check for Place Info API (mock: always healthy)");
		return true;
	}
}
