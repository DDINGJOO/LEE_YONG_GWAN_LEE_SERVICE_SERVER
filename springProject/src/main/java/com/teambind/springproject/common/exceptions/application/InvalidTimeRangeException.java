package com.teambind.springproject.common.exceptions.application;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 시간 범위가 유효하지 않을 때 발생하는 예외
 * HTTP 400 Bad Request
 */
public class InvalidTimeRangeException extends CustomException {
	
	public InvalidTimeRangeException() {
		super(ErrorCode.TIME_INVALID_RANGE);
	}
	
	public InvalidTimeRangeException(String message) {
		super(ErrorCode.TIME_INVALID_RANGE, message);
	}
	
	public static InvalidTimeRangeException endBeforeStart(String startTime, String endTime) {
		return new InvalidTimeRangeException(
				String.format("종료 시간이 시작 시간보다 이전입니다. 시작: %s, 종료: %s", startTime, endTime));
	}
	
	public static InvalidTimeRangeException endDateBeforeStartDate(String startDate, String endDate) {
		return new InvalidTimeRangeException(
				String.format("종료 날짜가 시작 날짜보다 이전입니다. 시작: %s, 종료: %s", startDate, endDate));
	}
	
	public static InvalidTimeRangeException invalidDuration(String duration) {
		return new InvalidTimeRangeException("잘못된 기간입니다: " + duration);
	}
	
	@Override
	public String getExceptionType() {
		return "APPLICATION";
	}
}
