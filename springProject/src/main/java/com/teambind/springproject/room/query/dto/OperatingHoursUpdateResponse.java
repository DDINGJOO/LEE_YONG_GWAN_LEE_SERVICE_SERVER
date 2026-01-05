package com.teambind.springproject.room.query.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 운영 시간 업데이트 응답 DTO.
 */
@Getter
@AllArgsConstructor
public class OperatingHoursUpdateResponse {

	/**
	 * 요청 ID (상태 조회에 사용)
	 */
	private String requestId;

	/**
	 * 룸 ID
	 */
	private Long roomId;
}
