package com.teambind.springproject.message.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이벤트 추상 클래스
 * 모든 도메인 이벤트의 기본 클래스
 * <p>
 * 불변 객체로 설계되어 이벤트의 무결성 보장
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Event {
	
	/**
	 * 이벤트가 발행될 Kafka 토픽
	 */
	private String topic;
}
