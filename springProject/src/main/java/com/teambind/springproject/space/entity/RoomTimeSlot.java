package com.teambind.springproject.space.entity;

import com.teambind.springproject.space.entity.enums.SlotStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 룸의 시간 슬롯을 나타내는 Entity.
 *
 * 운영 정책에 따라 생성되며, 예약 이벤트에 따라 상태가 변경된다. Rolling Window 방식으로 항상 2달치의 슬롯이
 * 유지된다.
 *
 * 상태 전이:
 *
 *
 * AVAILABLE → PENDING → RESERVED → CANCELLED
 *           ↓
 *       AVAILABLE (결제 실패 시)
 *
 */
@Entity
@Table(
		name = "room_time_slots",
		indexes = {
				@Index(name = "idx_room_date_time", columnList = "room_id,slot_date,slot_time"),
				@Index(name = "idx_date_status", columnList = "slot_date,status"),
				@Index(name = "idx_cleanup", columnList = "slot_date")
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
	 * @throws IllegalStateException 슬롯이 AVAILABLE 상태가 아닌 경우
	 */
	public void markAsPending(Long reservationId) {
		if (status != SlotStatus.AVAILABLE) {
			throw new IllegalStateException(
					String.format("Slot is not available. Current status: %s", status));
		}
		this.status = SlotStatus.PENDING;
		this.reservationId = Objects.requireNonNull(reservationId, "reservationId must not be null");
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 슬롯을 예약 확정 상태로 전환한다.
	 *
	 * @throws IllegalStateException 슬롯이 PENDING 상태가 아닌 경우
	 */
	public void confirm() {
		if (status != SlotStatus.PENDING) {
			throw new IllegalStateException(
					String.format("Slot is not pending. Current status: %s", status));
		}
		this.status = SlotStatus.RESERVED;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 예약을 취소하고 슬롯을 다시 예약 가능 상태로 전환한다.
	 *
	 * @throws IllegalStateException 슬롯이 PENDING 또는 RESERVED 상태가 아닌 경우
	 */
	public void cancel() {
		if (status != SlotStatus.PENDING && status != SlotStatus.RESERVED) {
			throw new IllegalStateException(
					String.format(
							"Cannot cancel slot. Current status: %s (expected PENDING or RESERVED)", status));
		}
		this.status = SlotStatus.CANCELLED;
		this.lastUpdated = LocalDateTime.now();
	}
	
	/**
	 * 취소된 슬롯을 다시 예약 가능 상태로 복구한다.
	 *
	 * @throws IllegalStateException 슬롯이 CANCELLED 상태가 아닌 경우
	 */
	public void restore() {
		if (status != SlotStatus.CANCELLED) {
			throw new IllegalStateException(
					String.format("Cannot restore slot. Current status: %s (expected CANCELLED)", status));
		}
		this.status = SlotStatus.AVAILABLE;
		this.reservationId = null;
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
		if (!(o instanceof RoomTimeSlot)) {
			return false;
		}
		RoomTimeSlot that = (RoomTimeSlot) o;
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
