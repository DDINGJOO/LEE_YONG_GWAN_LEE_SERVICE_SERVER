package com.teambind.springproject.room.domain.port;

import com.teambind.springproject.room.entity.RoomOperatingPolicy;

import java.util.List;
import java.util.Optional;

/**
 * 운영 정책 영속성 포트.
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
public interface OperatingPolicyPort {

	/**
	 * Room ID로 운영 정책을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @return 정책이 존재하면 Optional에 담아 반환, 없으면 빈 Optional
	 */
	Optional<RoomOperatingPolicy> findByRoomId(Long roomId);

	/**
	 * 운영 정책을 저장한다.
	 *
	 * @param policy 저장할 정책
	 * @return 저장된 정책
	 */
	RoomOperatingPolicy save(RoomOperatingPolicy policy);

	/**
	 * Room ID로 정책이 존재하는지 확인한다.
	 *
	 * @param roomId 룸 ID
	 * @return 정책이 존재하면 true, 아니면 false
	 */
	boolean existsByRoomId(Long roomId);

	/**
	 * Room ID로 정책을 삭제한다.
	 *
	 * @param roomId 룸 ID
	 */
	void deleteByRoomId(Long roomId);

	/**
	 * 모든 운영 정책을 조회한다.
	 *
	 * @return 모든 정책 목록
	 */
	List<RoomOperatingPolicy> findAll();
}
