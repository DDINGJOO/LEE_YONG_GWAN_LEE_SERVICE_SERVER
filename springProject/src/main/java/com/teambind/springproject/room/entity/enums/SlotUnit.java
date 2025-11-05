package com.teambind.springproject.room.entity.enums;

/**
 * 시간 슬롯의 단위를 나타내는 열거형.
 *
 * <p>Place Info API로부터 Room별로 조회되며, 슬롯 생성 시 사용된다.
 */
public enum SlotUnit {
	/**
	 * 1시간 단위 슬롯
	 */
	HOUR(60),
	
	/**
	 * 30분 단위 슬롯
	 */
	HALF_HOUR(30);
	
	private final int minutes;
	
	SlotUnit(int minutes) {
		this.minutes = minutes;
	}
	
	/**
	 * 슬롯 단위의 분(minute) 값을 반환한다.
	 *
	 * @return 분 단위 값
	 */
	public int getMinutes() {
		return minutes;
	}
}
