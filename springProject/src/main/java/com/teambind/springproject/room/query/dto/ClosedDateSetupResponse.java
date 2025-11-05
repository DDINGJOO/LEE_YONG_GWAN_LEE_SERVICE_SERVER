package com.teambind.springproject.room.query.dto;

import com.teambind.springproject.room.entity.enums.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 휴무일 설정 응답 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDateSetupResponse {

	/**
	 * 요청 ID
	 */
	private String requestId;

	/**
	 * 룸 ID
	 */
	private Long roomId;

	/**
	 * 설정된 휴무일 개수
	 */
	private Integer closedDateCount;

	/**
	 * 처리 상태
	 */
	private GenerationStatus status;

	/**
	 * 요청 시각
	 */
	private LocalDateTime requestedAt;
}
