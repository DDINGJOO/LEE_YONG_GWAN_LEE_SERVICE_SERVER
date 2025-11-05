package com.teambind.springproject.room.infrastructure.persistence;

import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.repository.RoomOperatingPolicyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OperatingPolicyPort의 JPA 구현체 (Adapter).
 *
 * <p>Hexagonal Architecture의 Adapter 패턴을 적용하여 도메인 계층과 인프라 계층을 분리한다.
 *
 * <p>SOLID 원칙:
 * <ul>
 *   <li>DIP (Dependency Inversion Principle): Port 인터페이스 구현으로 의존성 역전
 *   <li>SRP (Single Responsibility Principle): JPA 영속성 처리만 담당
 *   <li>OCP (Open-Closed Principle): 구현체 교체 가능 (JPA → MyBatis)
 * </ul>
 */
@Component
@Transactional
public class OperatingPolicyJpaAdapter implements OperatingPolicyPort {

	private final RoomOperatingPolicyRepository repository;

	public OperatingPolicyJpaAdapter(RoomOperatingPolicyRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RoomOperatingPolicy> findByRoomId(Long roomId) {
		return repository.findByRoomId(roomId);
	}

	@Override
	public RoomOperatingPolicy save(RoomOperatingPolicy policy) {
		return repository.save(policy);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByRoomId(Long roomId) {
		return repository.existsByRoomId(roomId);
	}

	@Override
	public void deleteByRoomId(Long roomId) {
		repository.deleteByRoomId(roomId);
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.List<RoomOperatingPolicy> findAll() {
		return repository.findAll();
	}
}
