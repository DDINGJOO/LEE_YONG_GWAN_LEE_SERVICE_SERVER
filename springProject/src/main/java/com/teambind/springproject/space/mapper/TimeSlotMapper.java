package com.teambind.springproject.space.mapper;

import com.teambind.springproject.space.dto.response.AvailableSlotResponse;
import com.teambind.springproject.space.dto.response.SlotAvailabilityResponse;
import com.teambind.springproject.space.dto.response.TimeSlotResponse;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RoomTimeSlot 엔티티와 DTO 간 변환을 담당하는 매퍼.
 */
@Component
public class TimeSlotMapper {
	
	/**
	 * RoomTimeSlot 엔티티를 TimeSlotResponse DTO로 변환한다.
	 *
	 * @param slot RoomTimeSlot 엔티티
	 * @return TimeSlotResponse DTO
	 */
	public TimeSlotResponse toTimeSlotResponse(RoomTimeSlot slot) {
		return new TimeSlotResponse(
				slot.getSlotId(),
				slot.getRoomId(),
				slot.getSlotDate(),
				slot.getSlotTime(),
				slot.getStatus(),
				slot.getReservationId()
		);
	}
	
	/**
	 * RoomTimeSlot 엔티티를 AvailableSlotResponse DTO로 변환한다.
	 *
	 * @param slot RoomTimeSlot 엔티티
	 * @return AvailableSlotResponse DTO
	 */
	public AvailableSlotResponse toAvailableSlotResponse(RoomTimeSlot slot) {
		return new AvailableSlotResponse(
				slot.getSlotId(),
				slot.getRoomId(),
				slot.getSlotDate(),
				slot.getSlotTime()
		);
	}
	
	/**
	 * RoomTimeSlot 엔티티 목록을 TimeSlotResponse DTO 목록으로 변환한다.
	 *
	 * @param slots RoomTimeSlot 엔티티 목록
	 * @return TimeSlotResponse DTO 목록
	 */
	public List<TimeSlotResponse> toTimeSlotResponseList(List<RoomTimeSlot> slots) {
		return slots.stream()
				.map(this::toTimeSlotResponse)
				.collect(Collectors.toList());
	}
	
	/**
	 * RoomTimeSlot 엔티티 목록을 AvailableSlotResponse DTO 목록으로 변환한다.
	 *
	 * @param slots RoomTimeSlot 엔티티 목록
	 * @return AvailableSlotResponse DTO 목록
	 */
	public List<AvailableSlotResponse> toAvailableSlotResponseList(List<RoomTimeSlot> slots) {
		return slots.stream()
				.map(this::toAvailableSlotResponse)
				.collect(Collectors.toList());
	}
	
	/**
	 * 슬롯 가용성 정보를 SlotAvailabilityResponse DTO로 변환한다.
	 *
	 * @param roomId    룸 ID
	 * @param slotDate  슬롯 날짜
	 * @param slotTime  슬롯 시간
	 * @param available 가용 여부
	 * @return SlotAvailabilityResponse DTO
	 */
	public SlotAvailabilityResponse toSlotAvailabilityResponse(
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			boolean available
	) {
		return new SlotAvailabilityResponse(
				roomId,
				slotDate,
				slotTime,
				available
		);
	}
}
