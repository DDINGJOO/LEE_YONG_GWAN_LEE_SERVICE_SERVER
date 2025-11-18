package com.teambind.springproject.room.command.dto;

import com.teambind.springproject.room.entity.enums.SlotUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 룸 운영 정책 설정 요청 DTO.
 * <p>
 * 초기 설정 시 운영 시간 정보를 받아 RoomOperatingPolicy에 저장한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomOperatingPolicySetupRequest {

	/**
	 * 룸 ID
	 */
	private Long roomId;

	/**
	 * 요일별 슬롯 시작 시각 목록 (각 슬롯에 recurrencePattern 포함)
	 */
	private List<WeeklySlotDto> slots;

	/**
	 * 슬롯 단위 (HOUR: 1시간, HALF_HOUR: 30분)
	 */
	private SlotUnit slotUnit;
}
