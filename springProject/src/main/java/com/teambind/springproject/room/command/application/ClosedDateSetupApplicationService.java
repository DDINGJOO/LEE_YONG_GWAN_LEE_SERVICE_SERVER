package com.teambind.springproject.room.command.application;

import com.teambind.springproject.common.exceptions.domain.RequestNotFoundException;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.dto.ClosedDateDto;
import com.teambind.springproject.room.command.dto.ClosedDateSetupRequest;
import com.teambind.springproject.room.domain.port.ClosedDateUpdateRequestPort;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.entity.ClosedDateUpdateRequest;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.vo.ClosedDateRange;
import com.teambind.springproject.room.event.event.ClosedDateUpdateRequestedEvent;
import com.teambind.springproject.room.query.dto.ClosedDateSetupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 휴무일 설정 Application Service.
 *
 * 룸의 휴무일을 설정하고, 이미 생성된 슬롯의 상태를 비동기로 업데이트한다.
 *
 * Hexagonal Architecture 적용:
 *
 * Infrastructure 계층(JPA)에 직접 의존하지 않고 Port 인터페이스에 의존
 * DIP (Dependency Inversion Principle) 준수
 * Use Case 조율만 담당 (비즈니스 로직은 DomainService/Entity에 위임)
 *
 */
@Slf4j
@Service
public class ClosedDateSetupApplicationService {
	
	private final OperatingPolicyPort operatingPolicyPort;
	private final ClosedDateUpdateRequestPort updateRequestPort;
	private final EventPublisher eventPublisher;
	
	public ClosedDateSetupApplicationService(
			OperatingPolicyPort operatingPolicyPort,
			ClosedDateUpdateRequestPort updateRequestPort,
			EventPublisher eventPublisher
	) {
		this.operatingPolicyPort = operatingPolicyPort;
		this.updateRequestPort = updateRequestPort;
		this.eventPublisher = eventPublisher;
	}
	
	/**
	 * 휴무일을 설정하고 슬롯 업데이트를 요청한다.
	 *
	 * 플로우:
	 * 1. RoomOperatingPolicy에 휴무일 추가
	 * 2. 업데이트 요청을 DB에 저장 (상태: REQUESTED)
	 * 3. Kafka 이벤트 발행
	 * 4. 즉시 응답 반환 (202 Accepted)
	 *
	 * @param request 휴무일 설정 요청
	 * @return 설정 응답 (요청 ID 포함)
	 */
	@Transactional
	public ClosedDateSetupResponse setupClosedDates(ClosedDateSetupRequest request) {
		log.info("Closed date setup requested: roomId={}, closedDateCount={}",
				request.getRoomId(), request.getClosedDates().size());
		
		// 1. RoomOperatingPolicy 조회 (Port 사용)
		RoomOperatingPolicy policy = operatingPolicyPort.findByRoomId(request.getRoomId())
				.orElseThrow(() -> new RequestNotFoundException(
						"Room operating policy not found for roomId: " + request.getRoomId()
				));
		
		// 2. ClosedDateRange 생성 및 정책에 추가
		List<ClosedDateRange> closedDateRanges = new ArrayList<>();
		for (ClosedDateDto dto : request.getClosedDates()) {
			ClosedDateRange range;
			
			// 패턴 기반 휴무일인 경우
			if (dto.getDayOfWeek() != null && dto.getRecurrencePattern() != null) {
				if (dto.getStartTime() != null && dto.getEndTime() != null) {
					// 패턴 기반 특정 시간 범위 휴무 (예: 매주 월요일 09:00~10:00)
					range = ClosedDateRange.ofPatternTimeRange(
							dto.getDayOfWeek(),
							dto.getRecurrencePattern(),
							dto.getStartTime(),
							dto.getEndTime()
					);
				} else {
					// 패턴 기반 하루 종일 휴무 (예: 매주 월요일 종일)
					range = ClosedDateRange.ofPatternFullDay(
							dto.getDayOfWeek(),
							dto.getRecurrencePattern()
					);
				}
			}
			// 날짜 기반 휴무일인 경우
			else {
				if (dto.getStartTime() != null && dto.getEndTime() != null) {
					// 특정 시간 범위 휴무
					range = ClosedDateRange.ofTimeRange(
							dto.getStartDate(),
							dto.getStartTime(),
							dto.getEndTime()
					);
				} else if (dto.getEndDate() != null) {
					// 날짜 범위 하루 종일 휴무
					range = ClosedDateRange.ofDateRange(
							dto.getStartDate(),
							dto.getEndDate()
					);
				} else {
					// 단일 날짜 하루 종일 휴무
					range = ClosedDateRange.ofFullDay(dto.getStartDate());
				}
			}
			
			closedDateRanges.add(range);
			policy.addClosedDate(range);
		}
		
		operatingPolicyPort.save(policy);
		
		log.info("Closed dates added to policy: roomId={}, policyId={}, count={}",
				request.getRoomId(), policy.getPolicyId(), closedDateRanges.size());
		
		// 3. 요청 ID 생성
		String requestId = UUID.randomUUID().toString();
		
		// 4. 업데이트 요청을 DB에 저장 (Port 사용)
		ClosedDateUpdateRequest updateRequest = ClosedDateUpdateRequest.create(
				requestId,
				request.getRoomId(),
				request.getClosedDates().size()
		);
		updateRequestPort.save(updateRequest);
		
		log.info("Closed date update request saved: requestId={}", requestId);
		
		// 5. Kafka 이벤트 발행 (경량 이벤트 - roomId만 전달)
		ClosedDateUpdateRequestedEvent event = ClosedDateUpdateRequestedEvent.of(
				requestId,
				request.getRoomId()
		);
		eventPublisher.publish(event);
		
		log.info("ClosedDateUpdateRequestedEvent published: requestId={}", requestId);
		
		// 6. 응답 생성
		return new ClosedDateSetupResponse(
				updateRequest.getRequestId(),
				updateRequest.getRoomId(),
				updateRequest.getClosedDateCount(),
				updateRequest.getStatus(),
				updateRequest.getRequestedAt()
		);
	}
}
