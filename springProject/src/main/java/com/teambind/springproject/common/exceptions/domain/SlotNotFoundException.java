package com.teambind.springproject.common.exceptions.domain;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 시간 슬롯을 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found
 */
public class SlotNotFoundException extends CustomException {
	
	public SlotNotFoundException() {
		super(ErrorCode.SLOT_NOT_FOUND);
	}
	
	public SlotNotFoundException(Long slotId) {
		super(ErrorCode.SLOT_NOT_FOUND, "슬롯을 찾을 수 없습니다. ID: " + slotId);
	}
	
	public SlotNotFoundException(Long roomId, String date, String time) {
		super(ErrorCode.SLOT_NOT_FOUND,
				String.format("슬롯을 찾을 수 없습니다. Room ID: %d, Date: %s, Time: %s", roomId, date, time));
	}
	
	@Override
	public String getExceptionType() {
		return "DOMAIN";
	}
}
