package com.teambind.springproject.space.dto;

import com.teambind.springproject.space.entity.enums.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 룸 초기 설정 응답 DTO.
 */
@Getter
@AllArgsConstructor
public class RoomSetupResponse {

	/**
	 * 요청 ID
	 */
	private String requestId;

	/**
	 * 룸 ID
	 */
	private Long roomId;

	/**
	 * 시작 날짜
	 */
	private LocalDate startDate;

	/**
	 * 종료 날짜
	 */
	private LocalDate endDate;

	/**
	 * 요청 상태
	 */
	private GenerationStatus status;

	/**
	 * 요청 시간
	 */
	private LocalDateTime requestedAt;
}