package com.teambind.springproject.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 휴무일 설정 요청 DTO.
 * <p>
 * 룸의 휴무일을 설정하고, 이미 생성된 슬롯의 상태를 CLOSED로 변경한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDateSetupRequest {

	/**
	 * 룸 ID
	 */
	private Long roomId;

	/**
	 * 휴무일 목록
	 */
	private List<ClosedDateDto> closedDates;
}
