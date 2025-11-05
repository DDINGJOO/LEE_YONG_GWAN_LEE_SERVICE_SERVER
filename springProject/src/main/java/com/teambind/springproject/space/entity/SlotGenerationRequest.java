package com.teambind.springproject.space.entity;

import com.teambind.springproject.space.entity.enums.GenerationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 슬롯 생성 요청 엔티티.
 * <p>
 * 비동기 슬롯 생성 작업의 상태를 추적한다.
 */
@Entity
@Table(name = "slot_generation_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotGenerationRequest {

	@Id
	@Column(length = 36)
	private String requestId;

	@Column(nullable = false)
	private Long roomId;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GenerationStatus status;

	@Column
	private Integer totalSlots;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	@Column
	private LocalDateTime startedAt;

	@Column
	private LocalDateTime completedAt;

	@Column(length = 1000)
	private String errorMessage;

	private SlotGenerationRequest(
			String requestId,
			Long roomId,
			LocalDate startDate,
			LocalDate endDate
	) {
		this.requestId = requestId;
		this.roomId = roomId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = GenerationStatus.REQUESTED;
		this.requestedAt = LocalDateTime.now();
	}

	/**
	 * 새로운 슬롯 생성 요청을 생성한다.
	 *
	 * @param requestId 요청 ID (UUID)
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @return 생성된 요청
	 */
	public static SlotGenerationRequest create(
			String requestId,
			Long roomId,
			LocalDate startDate,
			LocalDate endDate
	) {
		return new SlotGenerationRequest(requestId, roomId, startDate, endDate);
	}

	/**
	 * 처리 시작 상태로 전환한다.
	 */
	public void markAsInProgress() {
		this.status = GenerationStatus.IN_PROGRESS;
		this.startedAt = LocalDateTime.now();
	}

	/**
	 * 완료 상태로 전환한다.
	 *
	 * @param totalSlots 생성된 슬롯 개수
	 */
	public void markAsCompleted(int totalSlots) {
		this.status = GenerationStatus.COMPLETED;
		this.totalSlots = totalSlots;
		this.completedAt = LocalDateTime.now();
	}

	/**
	 * 실패 상태로 전환한다.
	 *
	 * @param errorMessage 에러 메시지
	 */
	public void markAsFailed(String errorMessage) {
		this.status = GenerationStatus.FAILED;
		this.errorMessage = errorMessage;
		this.completedAt = LocalDateTime.now();
	}
}