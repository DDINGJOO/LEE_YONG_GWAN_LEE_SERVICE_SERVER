package com.teambind.springproject.message.handler;

import com.teambind.springproject.message.event.Event;

/**
 * 이벤트 핸들러 인터페이스
 * <p>
 * 각 이벤트 타입별로 구현하여 이벤트 처리 로직을 정의한다.
 *
 * @param <T> 처리할 이벤트 타입
 */
public interface EventHandler<T extends Event> {
	
	/**
	 * 이벤트를 처리한다.
	 *
	 * @param event 처리할 이벤트
	 */
	void handle(T event);
	
	/**
	 * 이 핸들러가 처리할 수 있는 이벤트 타입을 반환한다.
	 *
	 * @return 이벤트 타입명
	 */
	String getSupportedEventType();
}
