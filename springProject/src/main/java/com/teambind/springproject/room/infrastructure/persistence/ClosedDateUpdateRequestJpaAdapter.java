package com.teambind.springproject.room.infrastructure.persistence;

import com.teambind.springproject.room.domain.port.ClosedDateUpdateRequestPort;
import com.teambind.springproject.room.entity.ClosedDateUpdateRequest;
import com.teambind.springproject.room.repository.ClosedDateUpdateRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * ClosedDateUpdateRequestPort의 JPA 구현체 (Adapter).
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
public class ClosedDateUpdateRequestJpaAdapter implements ClosedDateUpdateRequestPort {

	private final ClosedDateUpdateRequestRepository repository;

	public ClosedDateUpdateRequestJpaAdapter(ClosedDateUpdateRequestRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ClosedDateUpdateRequest> findById(String requestId) {
		return repository.findById(requestId);
	}

	@Override
	public ClosedDateUpdateRequest save(ClosedDateUpdateRequest request) {
		return repository.save(request);
	}
}
