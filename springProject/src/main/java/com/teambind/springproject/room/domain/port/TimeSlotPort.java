package com.teambind.springproject.room.domain.port;

import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 시간 슬롯 영속성 포트.
 *
 * <p>Hexagonal Architecture의 Port 인터페이스로, 도메인 계층이 인프라 계층에 의존하지 않도록
 * 추상화를 제공한다.
 *
 * <p>SOLID 원칙:
 * <ul>
 *   <li>DIP (Dependency Inversion Principle): 도메인이 구체적인 JPA 구현체가 아닌 이 인터페이스에 의존
 *   <li>ISP (Interface Segregation Principle): 도메인에 필요한 메서드만 정의
 * </ul>
 */
public interface TimeSlotPort {

	/**
	 * Room ID와 날짜, 시간으로 슬롯을 조회한다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param slotTime 슬롯 시간
	 * @return 슬롯이 존재하면 Optional에 담아 반환, 없으면 빈 Optional
	 */
	Optional<RoomTimeSlot> findByRoomIdAndSlotDateAndSlotTime(
			Long roomId, LocalDate slotDate, LocalTime slotTime);

	/**
	 * Room ID와 날짜 범위로 슬롯 목록을 조회한다.
	 *
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜 (inclusive)
	 * @param endDate   종료 날짜 (inclusive)
	 * @return 조회된 슬롯 목록
	 */
	List<RoomTimeSlot> findByRoomIdAndSlotDateBetween(
			Long roomId, LocalDate startDate, LocalDate endDate);

	/**
	 * Room ID와 날짜, 상태로 슬롯 목록을 조회한다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param status   슬롯 상태
	 * @return 조회된 슬롯 목록
	 */
	List<RoomTimeSlot> findByRoomIdAndSlotDateAndStatus(
			Long roomId, LocalDate slotDate, SlotStatus status);

	/**
	 * Reservation ID로 슬롯 목록을 조회한다.
	 *
	 * @param reservationId 예약 ID
	 * @return 조회된 슬롯 목록
	 */
	List<RoomTimeSlot> findByReservationId(Long reservationId);

	/**
	 * 슬롯을 저장한다.
	 *
	 * @param slot 저장할 슬롯
	 * @return 저장된 슬롯
	 */
	RoomTimeSlot save(RoomTimeSlot slot);

	/**
	 * 여러 슬롯을 한 번에 저장한다.
	 *
	 * @param slots 저장할 슬롯 목록
	 * @return 저장된 슬롯 목록
	 */
	List<RoomTimeSlot> saveAll(List<RoomTimeSlot> slots);

	/**
	 * 특정 날짜 이전의 모든 슬롯을 삭제한다.
	 *
	 * @param date 기준 날짜 (exclusive)
	 * @return 삭제된 슬롯 개수
	 */
	int deleteBySlotDateBefore(LocalDate date);

	/**
	 * Room ID로 모든 슬롯을 삭제한다.
	 *
	 * @param roomId 룸 ID
	 */
	void deleteByRoomId(Long roomId);

	/**
	 * Room ID와 날짜 범위, 상태로 슬롯 개수를 조회한다.
	 *
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @param status    슬롯 상태
	 * @return 조회된 슬롯 개수
	 */
	long countByRoomIdAndDateRangeAndStatus(
			Long roomId, LocalDate startDate, LocalDate endDate, SlotStatus status);

	/**
	 * 만료된 PENDING 슬롯을 조회한다.
	 *
	 * @param expirationMinutes PENDING 상태 유지 시간 (분)
	 * @return 만료된 슬롯 목록
	 */
	List<RoomTimeSlot> findExpiredPendingSlots(int expirationMinutes);
}
