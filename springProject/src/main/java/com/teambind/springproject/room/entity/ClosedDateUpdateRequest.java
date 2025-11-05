package com.teambind.springproject.room.entity;

import com.teambind.springproject.room.entity.enums.GenerationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 휴무일 업데이트 요청 엔티티.
 * <p>
 * 비동기 휴무일 업데이트 작업의 상태를 추적한다.
 */
@Entity
@Table(name = "closed_date_update_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClosedDateUpdateRequest {

	@Id
	@Column(length = 36)
	private String requestId;

	@Column(nullable = false)
	private Long roomId;

	@Column(nullable = false)
	private Integer closedDateCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GenerationStatus status;

	@Column
	private Integer affectedSlots;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	@Column
	private LocalDateTime startedAt;

	@Column
	private LocalDateTime completedAt;

	@Column(length = 1000)
	private String errorMessage;

	private ClosedDateUpdateRequest(
			String requestId,
			Long roomId,
			Integer closedDateCount
	) {
		this.requestId = requestId;
		this.roomId = roomId;
		this.closedDateCount = closedDateCount;
		this.status = GenerationStatus.REQUESTED;
		this.requestedAt = LocalDateTime.now();
	}

	/**
	 * 새로운 휴무일 업데이트 요청을 생성한다.
	 *
	 * @param requestId       요청 ID (UUID)
	 * @param roomId          룸 ID
	 * @param closedDateCount 휴무일 개수
	 * @return 생성된 요청
	 */
	public static ClosedDateUpdateRequest create(
			String requestId,
			Long roomId,
			Integer closedDateCount
	) {
		return new ClosedDateUpdateRequest(requestId, roomId, closedDateCount);
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
	 * @param affectedSlots 영향받은 슬롯 개수
	 */
	public void markAsCompleted(int affectedSlots) {
		this.status = GenerationStatus.COMPLETED;
		this.affectedSlots = affectedSlots;
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
