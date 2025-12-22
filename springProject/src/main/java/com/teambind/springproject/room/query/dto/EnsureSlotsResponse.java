package com.teambind.springproject.room.query.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 슬롯 보장 응답 DTO.
 * <p>
 * ensureSlotsForNext30Days API 호출 결과를 담는다.
 */
@Getter
@AllArgsConstructor
public class EnsureSlotsResponse {
	
	/**
	 * 룸 ID
	 */
	private Long roomId;
	
	/**
	 * 새로 생성된 슬롯 개수
	 */
	private int generatedCount;
}
