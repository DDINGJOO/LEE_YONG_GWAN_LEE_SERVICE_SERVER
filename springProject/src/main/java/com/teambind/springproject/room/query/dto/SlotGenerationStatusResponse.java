package com.teambind.springproject.room.query.dto;

import com.teambind.springproject.room.entity.SlotGenerationRequest;
import com.teambind.springproject.room.entity.enums.GenerationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 슬롯 생성 상태 조회 응답 DTO.
 */
@Getter
@AllArgsConstructor
public class SlotGenerationStatusResponse {

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
	 * 생성된 슬롯 개수
	 */
	private Integer totalSlots;

	/**
	 * 요청 시간
	 */
	private LocalDateTime requestedAt;

	/**
	 * 시작 시간
	 */
	private LocalDateTime startedAt;

	/**
	 * 완료 시간
	 */
	private LocalDateTime completedAt;

	/**
	 * 에러 메시지
	 */
	private String errorMessage;

	/**
	 * Entity로부터 DTO를 생성한다.
	 *
	 * @param request 슬롯 생성 요청 엔티티
	 * @return 상태 응답 DTO
	 */
	public static SlotGenerationStatusResponse from(SlotGenerationRequest request) {
		return new SlotGenerationStatusResponse(
				request.getRequestId(),
				request.getRoomId(),
				request.getStartDate(),
				request.getEndDate(),
				request.getStatus(),
				request.getTotalSlots(),
				request.getRequestedAt(),
				request.getStartedAt(),
				request.getCompletedAt(),
				request.getErrorMessage()
		);
	}
}