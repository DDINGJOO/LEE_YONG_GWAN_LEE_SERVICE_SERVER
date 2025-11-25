package com.teambind.springproject.room.entity.enums;

/**
 * 시간 슬롯의 상태를 나타내는 열거형.
 * <p>
 * 슬롯의 생명주기:
 * <pre>
 * AVAILABLE → PENDING → RESERVED
 *                ↓           ↓
 *            AVAILABLE   AVAILABLE (취소/환불 시)
 *
 * CLOSED ↔ AVAILABLE (휴무일 설정/해제)
 * </pre>
 */
public enum SlotStatus {
	/**
	 * 예약 가능 상태 , 혹은 취소됨
	 */
	AVAILABLE,
	
	/**
	 * 예약 진행중 (결제 대기)
	 */
	PENDING,
	
	/**
	 * 예약 확정
	 */
	RESERVED, // 결제완료
	
	
	/**
	 * 운영하지 않음 (휴무일)
	 */
	CLOSED
}
