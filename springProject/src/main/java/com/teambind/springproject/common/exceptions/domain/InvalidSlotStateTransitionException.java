package com.teambind.springproject.common.exceptions.domain;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 슬롯 상태 전이가 유효하지 않을 때 발생하는 예외
 * HTTP 400 Bad Request
 */
public class InvalidSlotStateTransitionException extends CustomException {
	
	public InvalidSlotStateTransitionException() {
		super(ErrorCode.SLOT_INVALID_STATE_TRANSITION);
	}
	
	public InvalidSlotStateTransitionException(String currentState, String targetState) {
		super(ErrorCode.SLOT_INVALID_STATE_TRANSITION,
				String.format("잘못된 상태 전이입니다. 현재 상태: %s, 목표 상태: %s", currentState, targetState));
	}
	
	public InvalidSlotStateTransitionException(String message) {
		super(ErrorCode.SLOT_INVALID_STATE_TRANSITION, message);
	}
	
	@Override
	public String getExceptionType() {
		return "DOMAIN";
	}
}
