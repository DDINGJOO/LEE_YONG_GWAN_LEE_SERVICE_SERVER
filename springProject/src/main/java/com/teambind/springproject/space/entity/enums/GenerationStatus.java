package com.teambind.springproject.space.entity.enums;

/**
 * 슬롯 생성 요청 상태.
 */
public enum GenerationStatus {

	/**
	 * 요청됨 - 처리 대기 중
	 */
	REQUESTED,

	/**
	 * 처리 중
	 */
	IN_PROGRESS,

	/**
	 * 완료됨
	 */
	COMPLETED,

	/**
	 * 실패함
	 */
	FAILED
}