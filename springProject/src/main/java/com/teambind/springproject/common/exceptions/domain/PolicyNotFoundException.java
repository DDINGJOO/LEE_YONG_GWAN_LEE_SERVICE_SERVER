package com.teambind.springproject.common.exceptions.domain;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 운영 정책을 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found
 */
public class PolicyNotFoundException extends CustomException {
	
	public PolicyNotFoundException() {
		super(ErrorCode.POLICY_NOT_FOUND);
	}
	
	public PolicyNotFoundException(Long policyId) {
		super(ErrorCode.POLICY_NOT_FOUND, "운영 정책을 찾을 수 없습니다. Policy ID: " + policyId);
	}
	
	public PolicyNotFoundException(Long roomId, boolean byRoomId) {
		super(ErrorCode.POLICY_NOT_FOUND, "운영 정책을 찾을 수 없습니다. Room ID: " + roomId);
	}
	
	@Override
	public String getExceptionType() {
		return "DOMAIN";
	}
}
