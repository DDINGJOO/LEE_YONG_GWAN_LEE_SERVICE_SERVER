package com.teambind.springproject.common.exceptions.application;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 과거 날짜가 허용되지 않을 때 발생하는 예외
 * HTTP 400 Bad Request
 */
public class PastDateNotAllowedException extends CustomException {
	
	public PastDateNotAllowedException() {
		super(ErrorCode.TIME_PAST_DATE_NOT_ALLOWED);
	}
	
	public PastDateNotAllowedException(String date) {
		super(ErrorCode.TIME_PAST_DATE_NOT_ALLOWED, "과거 날짜는 허용되지 않습니다: " + date);
	}
	
	public PastDateNotAllowedException(String fieldName, String date) {
		super(ErrorCode.TIME_PAST_DATE_NOT_ALLOWED,
				String.format("%s에 과거 날짜는 허용되지 않습니다: %s", fieldName, date));
	}
	
	@Override
	public String getExceptionType() {
		return "APPLICATION";
	}
}
