package com.teambind.springproject.space.repository;

import com.teambind.springproject.space.entity.RoomOperatingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RoomOperatingPolicy에 대한 데이터 접근 계층.
 *
 * 주요 책임:
 *
 *
 *   정책 CRUD
 *   Room ID로 정책 조회
 *
 */
@Repository
public interface RoomOperatingPolicyRepository extends JpaRepository<RoomOperatingPolicy, Long> {
	
	/**
	 * Room ID로 운영 정책을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @return 정책이 존재하면 Optional에 담아 반환, 없으면 빈 Optional
	 */
	Optional<RoomOperatingPolicy> findByRoomId(Long roomId);
	
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
}
