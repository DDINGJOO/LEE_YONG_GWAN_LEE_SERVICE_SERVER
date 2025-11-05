package com.teambind.springproject.room.repository;

import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * RoomTimeSlot에 대한 데이터 접근 계층.
 *
 * 주요 책임:
 *
 *
 *   슬롯 조회 (Room, 날짜, 시간, 상태별)
 *   슬롯 배치 생성/삭제
 *   Rolling Window 유지 (어제 슬롯 삭제)
 * 
 */
@Repository
public interface RoomTimeSlotRepository extends JpaRepository<RoomTimeSlot, Long> {
	
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
	 * 특정 날짜의 모든 슬롯을 삭제한다. (Rolling Window 유지용)
	 *
	 * @param slotDate 삭제할 슬롯 날짜
	 */
	@Modifying
	@Query("DELETE FROM RoomTimeSlot r WHERE r.slotDate = :slotDate")
	void deleteBySlotDate(@Param("slotDate") LocalDate slotDate);
	
	/**
	 * Room ID로 모든 슬롯을 삭제한다.
	 *
	 * @param roomId 룸 ID
	 */
	@Modifying
	@Query("DELETE FROM RoomTimeSlot r WHERE r.roomId = :roomId")
	void deleteByRoomId(@Param("roomId") Long roomId);
	
	/**
	 * 특정 날짜 이전의 모든 슬롯을 삭제한다.
	 *
	 * @param date 기준 날짜 (exclusive)
	 * @return 삭제된 슬롯 개수
	 */
	@Modifying
	@Query("DELETE FROM RoomTimeSlot r WHERE r.slotDate < :date")
	int deleteBySlotDateBefore(@Param("date") LocalDate date);
	
	/**
	 * Room ID와 날짜 범위, 상태로 예약 가능한 슬롯 수를 조회한다.
	 *
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @param status    슬롯 상태
	 * @return 조회된 슬롯 개수
	 */
	@Query(
			"SELECT COUNT(r) FROM RoomTimeSlot r "
					+ "WHERE r.roomId = :roomId "
					+ "AND r.slotDate BETWEEN :startDate AND :endDate "
					+ "AND r.status = :status")
	long countByRoomIdAndDateRangeAndStatus(
			@Param("roomId") Long roomId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("status") SlotStatus status);

	/**
	 * 만료된 PENDING 슬롯을 조회한다.
	 *
	 * @param status          슬롯 상태 (PENDING)
	 * @param expirationTime 만료 시간
	 * @return 만료된 슬롯 목록
	 */
	@Query("SELECT r FROM RoomTimeSlot r WHERE r.status = :status AND r.lastUpdated < :expirationTime")
	List<RoomTimeSlot> findByStatusAndLastUpdatedBefore(
			@Param("status") SlotStatus status,
			@Param("expirationTime") java.time.LocalDateTime expirationTime);
}
