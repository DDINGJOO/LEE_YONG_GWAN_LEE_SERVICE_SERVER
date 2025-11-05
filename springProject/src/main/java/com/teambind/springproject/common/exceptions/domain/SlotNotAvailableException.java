package com.teambind.springproject.common.exceptions.domain;

import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 슬롯이 예약 가능하지 않을 때 발생하는 예외
 * HTTP 409 Conflict
 */
public class SlotNotAvailableException extends CustomException {
	
	public SlotNotAvailableException() {
		super(ErrorCode.SLOT_NOT_AVAILABLE);
	}
	
	public SlotNotAvailableException(String currentStatus) {
		super(ErrorCode.SLOT_NOT_AVAILABLE,
				"슬롯을 예약할 수 없습니다. 현재 상태: " + currentStatus);
	}
	
	public SlotNotAvailableException(Long slotId, String currentStatus) {
		super(ErrorCode.SLOT_NOT_AVAILABLE,
				String.format("슬롯을 예약할 수 없습니다. Slot ID: %d, 현재 상태: %s", slotId, currentStatus));
	}
	
	@Override
	public String getExceptionType() {
		return "DOMAIN";
	}
}
