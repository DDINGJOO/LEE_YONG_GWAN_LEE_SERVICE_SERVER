package com.teambind.springproject.room.infrastructure.persistence;

import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * TimeSlotPort의 JPA 구현체 (Adapter).
 *
 * <p>Hexagonal Architecture의 Adapter 패턴을 적용하여 도메인 계층과 인프라 계층을 분리한다.
 *
 * <p>SOLID 원칙:
 * <ul>
 *   <li>DIP (Dependency Inversion Principle): Port 인터페이스 구현으로 의존성 역전
 *   <li>SRP (Single Responsibility Principle): JPA 영속성 처리만 담당
 *   <li>OCP (Open-Closed Principle): 구현체 교체 가능 (JPA → MyBatis)
 * </ul>
 */
@Component
@Transactional
public class TimeSlotJpaAdapter implements TimeSlotPort {

	private final RoomTimeSlotRepository repository;

	public TimeSlotJpaAdapter(RoomTimeSlotRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RoomTimeSlot> findByRoomIdAndSlotDateAndSlotTime(
			Long roomId, LocalDate slotDate, LocalTime slotTime) {
		return repository.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomTimeSlot> findByRoomIdAndSlotDateBetween(
			Long roomId, LocalDate startDate, LocalDate endDate) {
		return repository.findByRoomIdAndSlotDateBetween(roomId, startDate, endDate);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomTimeSlot> findByRoomIdAndSlotDateAndStatus(
			Long roomId, LocalDate slotDate, SlotStatus status) {
		return repository.findByRoomIdAndSlotDateAndStatus(roomId, slotDate, status);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomTimeSlot> findByReservationId(Long reservationId) {
		return repository.findByReservationId(reservationId);
	}

	@Override
	public RoomTimeSlot save(RoomTimeSlot slot) {
		return repository.save(slot);
	}

	@Override
	public List<RoomTimeSlot> saveAll(List<RoomTimeSlot> slots) {
		return repository.saveAll(slots);
	}

	@Override
	public int deleteBySlotDateBefore(LocalDate date) {
		return repository.deleteBySlotDateBefore(date);
	}

	@Override
	public void deleteByRoomId(Long roomId) {
		repository.deleteByRoomId(roomId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countByRoomIdAndDateRangeAndStatus(
			Long roomId, LocalDate startDate, LocalDate endDate, SlotStatus status) {
		return repository.countByRoomIdAndDateRangeAndStatus(roomId, startDate, endDate, status);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomTimeSlot> findExpiredPendingSlots(int expirationMinutes) {
		LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(expirationMinutes);

		// 성능 최적화: Custom Query 사용 (findAll() + stream 제거)
		return repository.findByStatusAndLastUpdatedBefore(SlotStatus.PENDING, expirationTime);
	}
}
