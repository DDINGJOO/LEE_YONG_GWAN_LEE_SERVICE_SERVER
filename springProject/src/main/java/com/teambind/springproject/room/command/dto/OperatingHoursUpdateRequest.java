package com.teambind.springproject.room.command.dto;

import com.teambind.springproject.room.entity.enums.SlotUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 운영 시간 업데이트 요청 DTO.
 * <p>
 * 룸의 운영 시간(주간 스케줄, 슬롯 단위)을 변경하고 슬롯을 재생성한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHoursUpdateRequest {

	/**
	 * 룸 ID
	 */
	private Long roomId;

	/**
	 * 요일별 슬롯 시작 시각 목록
	 */
	private List<WeeklySlotDto> slots;

	/**
	 * 슬롯 단위 (HOUR: 1시간, HALF_HOUR: 30분)
	 */
	private SlotUnit slotUnit;
}
