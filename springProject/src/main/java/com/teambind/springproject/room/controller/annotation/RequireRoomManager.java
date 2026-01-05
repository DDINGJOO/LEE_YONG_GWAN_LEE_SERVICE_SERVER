package com.teambind.springproject.room.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * PLACE_MANAGER 권한이 필요한 API에 적용하는 어노테이션.
 * <p>
 * 이 어노테이션이 적용된 메서드는 X-App-Type 헤더가 PLACE_MANAGER인 경우에만 접근 가능하다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRoomManager {
}
