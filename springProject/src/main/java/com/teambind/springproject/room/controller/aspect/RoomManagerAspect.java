package com.teambind.springproject.room.controller.aspect;

import com.teambind.springproject.common.exceptions.application.ForbiddenException;
import com.teambind.springproject.common.exceptions.application.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * PLACE_MANAGER 권한 검증 AOP.
 * <p>
 * {@link com.teambind.springproject.room.controller.annotation.RequireRoomManager} 어노테이션이
 * 적용된 메서드에서 X-App-Type 헤더를 검증한다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RoomManagerAspect {

	private static final String APP_TYPE_HEADER = "X-App-Type";
	private static final String PLACE_MANAGER = "PLACE_MANAGER";

	@Before("@annotation(com.teambind.springproject.room.controller.annotation.RequireRoomManager)")
	public void checkRoomManagerPermission(JoinPoint joinPoint) {
		ServletRequestAttributes attributes =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		if (attributes == null) {
			log.warn("Request attributes not found");
			throw InvalidRequestException.requiredFieldMissing(APP_TYPE_HEADER);
		}

		HttpServletRequest request = attributes.getRequest();
		String appType = request.getHeader(APP_TYPE_HEADER);

		if (appType == null || appType.isBlank()) {
			log.warn("Missing {} header for method: {}",
					APP_TYPE_HEADER, joinPoint.getSignature().getName());
			throw InvalidRequestException.requiredFieldMissing(APP_TYPE_HEADER);
		}

		if (!PLACE_MANAGER.equals(appType)) {
			log.warn("Access denied: appType={} for method: {}",
					appType, joinPoint.getSignature().getName());
			throw ForbiddenException.insufficientPermission();
		}

		log.debug("Permission granted: appType={} for method: {}",
				appType, joinPoint.getSignature().getName());
	}
}
