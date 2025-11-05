package com.teambind.springproject.room.domain.port;

import com.teambind.springproject.room.entity.ClosedDateUpdateRequest;

import java.util.Optional;

/**
 * 휴무일 업데이트 요청 영속성 포트.
 *
 * <p>Hexagonal Architecture의 Port 인터페이스로, 도메인 계층이 인프라 계층에 의존하지 않도록
 * 추상화를 제공한다.
 *
 * <p>SOLID 원칙:
 * <ul>
 *   <li>DIP (Dependency Inversion Principle): 도메인이 구체적인 JPA 구현체가 아닌 이 인터페이스에 의존
 *   <li>ISP (Interface Segregation Principle): 도메인에 필요한 메서드만 정의
 * </ul>
 */
public interface ClosedDateUpdateRequestPort {

	/**
	 * Request ID로 휴무일 업데이트 요청을 조회한다.
	 *
	 * @param requestId 요청 ID
	 * @return 요청이 존재하면 Optional에 담아 반환, 없으면 빈 Optional
	 */
	Optional<ClosedDateUpdateRequest> findById(String requestId);

	/**
	 * 휴무일 업데이트 요청을 저장한다.
	 *
	 * @param request 저장할 요청
	 * @return 저장된 요청
	 */
	ClosedDateUpdateRequest save(ClosedDateUpdateRequest request);
}
