package com.teambind.springproject.common.exceptions.application;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 타임 슬롯 생성에 실패했을 때 발생하는 예외
 * HTTP 500 Internal Server Error
 */
public class SlotGenerationFailedException extends CustomException {
	
	public SlotGenerationFailedException() {
		super(ErrorCode.TIME_SLOT_GENERATION_FAILED);
	}
	
	public SlotGenerationFailedException(String message) {
		super(ErrorCode.TIME_SLOT_GENERATION_FAILED, message);
	}
	
	public SlotGenerationFailedException(String message, Throwable cause) {
		super(ErrorCode.TIME_SLOT_GENERATION_FAILED, message, cause);
	}
	
	public static SlotGenerationFailedException forDate(String date, Throwable cause) {
		return new SlotGenerationFailedException(
				"타임 슬롯 생성에 실패했습니다. 날짜: " + date, cause);
	}
	
	public static SlotGenerationFailedException forRoom(Long roomId, Throwable cause) {
		return new SlotGenerationFailedException(
				"타임 슬롯 생성에 실패했습니다. Room ID: " + roomId, cause);
	}
	
	@Override
	public String getExceptionType() {
		return "APPLICATION";
	}
}
