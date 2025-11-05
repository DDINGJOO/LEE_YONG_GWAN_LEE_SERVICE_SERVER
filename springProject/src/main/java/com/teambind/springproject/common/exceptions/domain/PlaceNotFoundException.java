package com.teambind.springproject.common.exceptions.domain;


import com.teambind.springproject.common.exceptions.CustomException;
import com.teambind.springproject.common.exceptions.ErrorCode;

/**
 * 장소를 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found
 */
public class PlaceNotFoundException extends CustomException {
	
	public PlaceNotFoundException() {
		super(ErrorCode.ROOM_NOT_FOUND);
	}
	
	public PlaceNotFoundException(String roomId) {
		super(ErrorCode.ROOM_NOT_FOUND, "장소를 찾을 수 없습니다. ID: " + roomId);
	}
	
	@Override
	public String getExceptionType() {
		return "DOMAIN";
	}
}
