package com.teambind.springproject.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * JPA 및 애플리케이션 타임존 설정.
 *
 * <p>모든 날짜/시간 데이터를 한국 표준시(KST, Asia/Seoul)로 통일합니다.
 *
 * <p>설정 범위:
 *
 * <ul>
 *   <li>JVM 기본 타임존: Asia/Seoul
 *   <li>JDBC 타임존: Asia/Seoul (application.yaml)
 *   <li>Hibernate 타임존: Asia/Seoul (application.yaml)
 *   <li>MySQL/MariaDB 세션 타임존: +09:00 (schema.sql)
 * </ul>
 */
@Configuration
public class JpaConfig {
	
	/**
	 * 애플리케이션 시작 시 JVM 기본 타임존을 Asia/Seoul로 설정한다.
	 *
	 * <p>이 설정은 다음에 영향을 줍니다:
	 *
	 * <ul>
	 *   <li>LocalDateTime.now() - 한국 시간 기준
	 *   <li>Instant.now() - UTC 기준 (변환 시 KST 사용)
	 *   <li>SimpleDateFormat - 기본 타임존 KST
	 * </ul>
	 */
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}
}
