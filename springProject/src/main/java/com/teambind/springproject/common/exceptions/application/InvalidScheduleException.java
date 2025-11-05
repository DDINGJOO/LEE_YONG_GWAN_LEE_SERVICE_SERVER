package com.teambind.springproject.common.exceptions.application;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 스케줄 설정이 유효하지 않을 때 발생하는 예외
 * HTTP 400 Bad Request
 */
public class InvalidScheduleException extends CustomException {
	
	public InvalidScheduleException() {
		super(ErrorCode.POLICY_INVALID_SCHEDULE);
	}
	
	public InvalidScheduleException(String message) {
		super(ErrorCode.POLICY_INVALID_SCHEDULE, message);
	}
	
	public static InvalidScheduleException emptySchedule() {
		return new InvalidScheduleException("스케줄이 비어있습니다. 최소 하나의 운영 시간이 필요합니다.");
	}
	
	public static InvalidScheduleException invalidSlotTime(String dayOfWeek, String time) {
		return new InvalidScheduleException(
				String.format("잘못된 슬롯 시간입니다. 요일: %s, 시간: %s", dayOfWeek, time));
	}
	
	public static InvalidScheduleException duplicateSlotTime(String dayOfWeek, String time) {
		return new InvalidScheduleException(
				String.format("중복된 슬롯 시간입니다. 요일: %s, 시간: %s", dayOfWeek, time));
	}
	
	@Override
	public String getExceptionType() {
		return "APPLICATION";
	}
}
