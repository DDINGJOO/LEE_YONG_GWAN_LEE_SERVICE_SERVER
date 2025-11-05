package com.teambind.springproject.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	// Place 관련 에러 (PLACE_0XX)

	// 권한 관련 에러 (AUTH_0XX)
	UNAUTHORIZED("AUTH_001", "Unauthorized access", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("AUTH_002", "Access forbidden", HttpStatus.FORBIDDEN),
	INSUFFICIENT_PERMISSION("AUTH_003", "Insufficient permission", HttpStatus.FORBIDDEN),

	// 검증 관련 에러 (VALIDATION_0XX)
	INVALID_INPUT("VALIDATION_001", "Invalid input", HttpStatus.BAD_REQUEST),
	REQUIRED_FIELD_MISSING("VALIDATION_002", "Required field is missing", HttpStatus.BAD_REQUEST),
	INVALID_FORMAT("VALIDATION_003", "Invalid format", HttpStatus.BAD_REQUEST),
	VALUE_OUT_OF_RANGE("VALIDATION_004", "Value is out of range", HttpStatus.BAD_REQUEST),

	// 시스템 에러 (SYSTEM_0XX)
	INTERNAL_SERVER_ERROR("SYSTEM_001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
	DATABASE_ERROR("SYSTEM_002", "Database error", HttpStatus.INTERNAL_SERVER_ERROR),
	EXTERNAL_API_ERROR("SYSTEM_003", "External API error", HttpStatus.BAD_GATEWAY),
	CACHE_ERROR("SYSTEM_004", "Cache error", HttpStatus.INTERNAL_SERVER_ERROR),
	EVENT_PUBLISH_FAILED("SYSTEM_005", "Failed to publish event", HttpStatus.INTERNAL_SERVER_ERROR),
	
	// Room 관련 에러 (ROOM_0XX)
	ROOM_NOT_FOUND("ROOM_001", "Room not found", HttpStatus.NOT_FOUND),
	
	// Slot 관련 에러 (SLOT_0XX)
	SLOT_NOT_FOUND("SLOT_001", "Slot not found", HttpStatus.NOT_FOUND),
	SLOT_NOT_AVAILABLE("SLOT_002", "Slot is not available", HttpStatus.CONFLICT),
	SLOT_ALREADY_RESERVED("SLOT_003", "Slot is already reserved", HttpStatus.CONFLICT),
	SLOT_INVALID_STATE_TRANSITION("SLOT_004", "Invalid slot state transition", HttpStatus.BAD_REQUEST),
	SLOT_ALREADY_CANCELLED("SLOT_005", "Slot is already cancelled", HttpStatus.BAD_REQUEST),
	SLOT_CANNOT_BE_MODIFIED("SLOT_006", "Slot cannot be modified", HttpStatus.BAD_REQUEST),
	
	// Policy 관련 에러 (POLICY_0XX)
	POLICY_NOT_FOUND("POLICY_001", "Operating policy not found", HttpStatus.NOT_FOUND),
	POLICY_ALREADY_EXISTS("POLICY_002", "Operating policy already exists", HttpStatus.CONFLICT),
	POLICY_INVALID_SCHEDULE("POLICY_003", "Invalid schedule configuration", HttpStatus.BAD_REQUEST),
	POLICY_INVALID_CLOSED_DATE("POLICY_004", "Invalid closed date range", HttpStatus.BAD_REQUEST),
	
	// Time 관련 에러 (TIME_0XX)
	TIME_INVALID_RANGE("TIME_001", "Invalid time range", HttpStatus.BAD_REQUEST),
	TIME_PAST_DATE_NOT_ALLOWED("TIME_002", "Past date is not allowed", HttpStatus.BAD_REQUEST),
	TIME_SLOT_GENERATION_FAILED("TIME_003", "Failed to generate time slots", HttpStatus.INTERNAL_SERVER_ERROR),
	;
	private final String errCode;
	private final String message;
	private final HttpStatus status;
	
	ErrorCode(String errCode, String message, HttpStatus status) {
		
		this.status = status;
		this.errCode = errCode;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "ErrorCode{"
				+ " status='"
				+ status
				+ '\''
				+ "errCode='"
				+ errCode
				+ '\''
				+ ", message='"
				+ message
				+ '\''
				+ '}';
	}
}
