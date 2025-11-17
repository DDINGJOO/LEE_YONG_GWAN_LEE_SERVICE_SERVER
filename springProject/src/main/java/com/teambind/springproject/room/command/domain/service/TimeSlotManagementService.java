package com.teambind.springproject.room.command.domain.service;

/**
 * 시간 슬롯 상태 관리 서비스.
 * <p>
 * 주요 책임:
 * <p>
 * <p>
 * 예약 이벤트에 따른 슬롯 상태 변경
 * Redis 캐시와 DB 동기화
 * 동시성 제어 (Optimistic/Pessimistic Lock)
 *
 */

public interface TimeSlotManagementService {
	
	/**
	 * 슬롯을 예약 대기 상태(PENDING)로 변경한다.
	 * <p>
	 * 처리 플로우:
	 * <p>
	 * <p>
	 * 슬롯 조회 및 가용성 체크
	 * AVAILABLE → PENDING 상태 전환
	 * Redis 캐시 업데이트
	 *
	 * @param roomId        룸 ID
	 * @param slotDate      슬롯 날짜
	 * @param slotTime      슬롯 시간
	 * @param reservationId 예약 ID
	 * @throws IllegalStateException 슬롯이 예약 불가능한 경우
	 */
	void markSlotAsPending(Long roomId, java.time.LocalDate slotDate, java.time.LocalTime slotTime, Long reservationId);
	
	/**
	 * 슬롯을 예약 확정 상태(RESERVED)로 변경한다.
	 * <p>
	 * 결제 완료 이벤트 처리 시 호출된다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param slotTime 슬롯 시간
	 * @throws IllegalStateException 슬롯이 PENDING 상태가 아닌 경우
	 */
	void confirmSlot(Long roomId, java.time.LocalDate slotDate, java.time.LocalTime slotTime);
	
	/**
	 * 슬롯을 취소하고 다시 예약 가능 상태로 복구한다.
	 * <p>
	 * 결제 실패 또는 예약 취소 이벤트 처리 시 호출된다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param slotTime 슬롯 시간
	 */
	void cancelSlot(Long roomId, java.time.LocalDate slotDate, java.time.LocalTime slotTime);
	
	/**
	 * 예약 ID로 연관된 모든 슬롯을 취소한다.
	 *
	 * @param reservationId 예약 ID
	 */
	void cancelSlotsByReservationId(Long reservationId);
	
	/**
	 * 만료된 PENDING 슬롯을 다시 AVAILABLE 상태로 복구한다.
	 * <p>
	 * 스케줄러가 주기적으로 호출한다. (예: 15분 이상 PENDING 상태인 슬롯)
	 *
	 * @return 복구된 슬롯 개수
	 */
	int restoreExpiredPendingSlots();

	/**
	 * 여러 슬롯을 한 번에 예약 대기 상태(PENDING)로 변경한다.
	 *
	 * 동시성 제어:
	 * - Pessimistic Lock (SELECT ... FOR UPDATE) 사용
	 * - 트랜잭션 내에서 모든 슬롯을 잠그고 상태 검증
	 * - 하나라도 예약 불가능하면 전체 롤백
	 *
	 * @param roomId        룸 ID
	 * @param slotDate      슬롯 날짜
	 * @param slotTimes     슬롯 시간 리스트
	 * @param reservationId 예약 ID
	 * @return 예약된 슬롯 개수
	 * @throws IllegalStateException 슬롯이 예약 불가능한 경우
	 */
	int markMultipleSlotsAsPending(
			Long roomId,
			java.time.LocalDate slotDate,
			java.util.List<java.time.LocalTime> slotTimes,
			Long reservationId
	);
}
