package com.teambind.springproject.room.entity;

import com.teambind.springproject.common.exceptions.application.InvalidRequestException;
import com.teambind.springproject.common.exceptions.domain.InvalidSlotStateTransitionException;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 룸의 시간 슬롯을 나타내는 Entity.
 * <p>
 * 운영 정책에 따라 생성되며, 예약 이벤트에 따라 상태가 변경된다.
 * Rolling Window 방식으로 설정값 기반(기본 30일)의 슬롯이 유지된다.
 * <p>
 * 상태 전이:
 * <pre>
 * AVAILABLE → PENDING → RESERVED
 *                ↓           ↓
 *            AVAILABLE   AVAILABLE (취소/환불 시)
 *
 * CLOSED ↔ AVAILABLE (휴무일 설정/해제)
 * </pre>
 */
@Entity
@Table(
		name = "room_time_slots",
		indexes = {
				@Index(name = "idx_room_date_time", columnList = "room_id,slot_date,slot_time"),
				@Index(name = "idx_date_status", columnList = "slot_date,status"),
				@Index(name = "idx_cleanup", columnList = "slot_date")
		},
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_room_date_time",
						columnNames = {"room_id", "slot_date", "slot_time"}
				)
		})
public class RoomTimeSlot {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long slotId;
	
	@Column(name = "room_id", nullable = false)
	private Long roomId;
	
	@Column(name = "slot_date", nullable = false)
	private LocalDate slotDate;
	
	@Column(name = "slot_time", nullable = false)
	private LocalTime slotTime;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SlotStatus status;
	
	@Column(name = "reservation_id")
	private Long reservationId;
	
	@Column(name = "last_updated", nullable = false)
	private LocalDateTime lastUpdated;
	
	protected RoomTimeSlot() {
		// JPA를 위한 기본 생성자
	}
	
	private RoomTimeSlot(
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			SlotStatus status,
			Long reservationId) {
		this.roomId = Objects.requireNonNull(roomId, "roomId must not be null");
		this.slotDate = Objects.requireNonNull(slotDate, "slotDate must not be null");
		this.slotTime = Objects.requireNonNull(slotTime, "slotTime must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.reservationId = reservationId;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 예약 가능한 슬롯을 생성한다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param slotTime 슬롯 시각
	 * @return 생성된 RoomTimeSlot (상태: AVAILABLE)
	 */
	public static RoomTimeSlot available(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		return new RoomTimeSlot(roomId, slotDate, slotTime, SlotStatus.AVAILABLE, null);
	}
	
	/**
	 * 휴무 슬롯을 생성한다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param slotTime 슬롯 시각
	 * @return 생성된 RoomTimeSlot (상태: CLOSED)
	 */
	public static RoomTimeSlot closed(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		return new RoomTimeSlot(roomId, slotDate, slotTime, SlotStatus.CLOSED, null);
	}
	
	/**
	 * 슬롯을 예약 대기 상태로 전환한다.
	 *
	 * @param reservationId 예약 ID
	 * @throws InvalidSlotStateTransitionException 슬롯이 AVAILABLE 상태가 아닌 경우
	 * @throws InvalidRequestException             reservationId가 null인 경우
	 */
	public void markAsPending(Long reservationId) {
		if (status != SlotStatus.AVAILABLE) {
			throw new InvalidSlotStateTransitionException(
					status.name(), SlotStatus.PENDING.name());
		}
		if (reservationId == null) {
			throw InvalidRequestException.requiredFieldMissing("reservationId");
		}
		this.status = SlotStatus.PENDING;
		this.reservationId = reservationId;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 슬롯을 예약 확정 상태로 전환한다.
	 *
	 * @throws InvalidSlotStateTransitionException 슬롯이 PENDING 상태가 아닌 경우
	 */
	public void confirm() {
		if (status != SlotStatus.PENDING) {
			throw new InvalidSlotStateTransitionException(
					status.name(), SlotStatus.RESERVED.name());
		}
		this.status = SlotStatus.RESERVED;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 예약을 취소하고 슬롯을 다시 예약 가능 상태로 전환한다.
	 *
	 * @throws InvalidSlotStateTransitionException 슬롯이 PENDING 또는 RESERVED 상태가 아닌 경우
	 */
	public void cancel() {
		if (status != SlotStatus.PENDING && status != SlotStatus.RESERVED) {
			throw new InvalidSlotStateTransitionException(
					String.format("슬롯을 취소할 수 없습니다. 현재 상태: %s (PENDING 또는 RESERVED 상태여야 함)",
							status.name()));
		}
		this.reservationId = null;
		this.status = SlotStatus.AVAILABLE;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 슬롯을 휴무 상태로 전환한다.
	 * <p>
	 * 휴무일 설정 시 호출되며, AVAILABLE 상태의 슬롯만 CLOSED로 변경할 수 있다.
	 *
	 * @throws InvalidSlotStateTransitionException 슬롯이 AVAILABLE 상태가 아닌 경우
	 */
	public void markAsClosed() {
		if (status != SlotStatus.AVAILABLE) {
			throw new InvalidSlotStateTransitionException(
					status.name(), SlotStatus.CLOSED.name());
		}
		this.status = SlotStatus.CLOSED;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 슬롯을 예약 가능 상태로 전환한다.
	 * <p>
	 * 다음 시나리오에서 호출된다:
	 * - 휴무일 해제 시 (CLOSED → AVAILABLE)
	 * - 환불 완료 시 (RESERVED → AVAILABLE)
	 * - 결제 만료 시 (PENDING → AVAILABLE)
	 * <p>
	 * 이미 AVAILABLE 상태인 경우 멱등성(idempotent)을 보장하여 예외를 발생시키지 않는다.
	 */
	public void markAsAvailable() {
		// 이미 AVAILABLE이면 멱등성 보장 (환불 재시도 등)
		if (status == SlotStatus.AVAILABLE) {
			return;
		}
		
		// RESERVED, PENDING, CLOSED → AVAILABLE 전이 허용
		if (status != SlotStatus.RESERVED &&
				status != SlotStatus.PENDING &&
				status != SlotStatus.CLOSED) {
			throw new InvalidSlotStateTransitionException(
					status.name(), SlotStatus.AVAILABLE.name());
		}

		this.status = SlotStatus.AVAILABLE;
		this.reservationId = null; // 예약 ID 초기화
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 슬롯이 예약 가능한지 확인한다.
	 *
	 * @return 상태가 AVAILABLE이면 true, 아니면 false
	 */
	public boolean isAvailable() {
		return status == SlotStatus.AVAILABLE;
	}
	
	/**
	 * 슬롯이 예약됨 상태인지 확인한다.
	 *
	 * @return 상태가 RESERVED이면 true, 아니면 false
	 */
	public boolean isReserved() {
		return status == SlotStatus.RESERVED;
	}
	
	// Getters
	public Long getSlotId() {
		return slotId;
	}
	
	public Long getRoomId() {
		return roomId;
	}
	
	public LocalDate getSlotDate() {
		return slotDate;
	}
	
	public LocalTime getSlotTime() {
		return slotTime;
	}
	
	public SlotStatus getStatus() {
		return status;
	}
	
	public Long getReservationId() {
		return reservationId;
	}
	
	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RoomTimeSlot that)) {
			return false;
		}
		return Objects.equals(slotId, that.slotId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(slotId);
	}
	
	@Override
	public String toString() {
		return String.format(
				"RoomTimeSlot{roomId=%d, date=%s, time=%s, status=%s}",
				roomId, slotDate, slotTime, status);
	}
}
