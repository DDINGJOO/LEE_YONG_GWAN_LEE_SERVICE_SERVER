package com.teambind.springproject.common.exceptions.domain;

/**
 * 요청을 찾을 수 없을 때 발생하는 예외.
 */
public class RequestNotFoundException extends RuntimeException {

	public RequestNotFoundException(String message) {
		super(message);
	}
}