package com.teambind.springproject.room.entity.enums;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * 운영 시간 반복 패턴을 나타내는 열거형.
 *
 * <p>각 패턴은 특정 날짜가 해당 패턴과 일치하는지 판단하는 로직을 포함한다.
 */
public enum RecurrencePattern {
	/**
	 * 매주 반복
	 */
	EVERY_WEEK {
		@Override
		public boolean matches(LocalDate date) {
			return true;
		}
	},
	
	/**
	 * 홀수 주에만 운영 (ISO 8601 Week-based year 기준)
	 */
	ODD_WEEK {
		@Override
		public boolean matches(LocalDate date) {
			int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			return weekOfYear % 2 == 1;
		}
	},
	
	/**
	 * 짝수 주에만 운영 (ISO 8601 Week-based year 기준)
	 */
	EVEN_WEEK {
		@Override
		public boolean matches(LocalDate date) {
			int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
			return weekOfYear % 2 == 0;
		}
	};
	
	/**
	 * 주어진 날짜가 이 반복 패턴과 일치하는지 확인한다.
	 *
	 * @param date 확인할 날짜
	 * @return 패턴과 일치하면 true, 아니면 false
	 */
	public abstract boolean matches(LocalDate date);
}
