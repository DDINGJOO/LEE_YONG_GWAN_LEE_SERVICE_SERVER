package com.teambind.springproject.common.exceptions.domain;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 운영 정책이 이미 존재할 때 발생하는 예외
 * HTTP 409 Conflict
 */
public class PolicyAlreadyExistsException extends CustomException {
	
	public PolicyAlreadyExistsException() {
		super(ErrorCode.POLICY_ALREADY_EXISTS);
	}
	
	public PolicyAlreadyExistsException(Long roomId) {
		super(ErrorCode.POLICY_ALREADY_EXISTS, "운영 정책이 이미 존재합니다. Room ID: " + roomId);
	}
	
	@Override
	public String getExceptionType() {
		return "DOMAIN";
	}
}
