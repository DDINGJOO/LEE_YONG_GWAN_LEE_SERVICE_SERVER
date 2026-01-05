package com.teambind.springproject.common.exceptions.application;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 접근 권한이 없을 때 발생하는 예외
 * HTTP 403 Forbidden
 */
public class ForbiddenException extends CustomException {

	public ForbiddenException() {
		super(ErrorCode.FORBIDDEN);
	}

	public ForbiddenException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ForbiddenException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static ForbiddenException insufficientPermission() {
		return new ForbiddenException(
				ErrorCode.INSUFFICIENT_PERMISSION,
				"해당 작업을 수행할 권한이 없습니다. PLACE_MANAGER 권한이 필요합니다."
		);
	}

	@Override
	public String getExceptionType() {
		return "APPLICATION";
	}
}
