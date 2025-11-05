package com.teambind.springproject.space.service;

import com.teambind.springproject.common.exceptions.domain.RequestNotFoundException;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.space.dto.ClosedDateDto;
import com.teambind.springproject.space.dto.ClosedDateSetupRequest;
import com.teambind.springproject.space.dto.ClosedDateSetupResponse;
import com.teambind.springproject.space.entity.ClosedDateUpdateRequest;
import com.teambind.springproject.space.entity.RoomOperatingPolicy;
import com.teambind.springproject.space.entity.vo.ClosedDateRange;
import com.teambind.springproject.space.event.ClosedDateUpdateRequestedEvent;
import com.teambind.springproject.space.repository.ClosedDateUpdateRequestRepository;
import com.teambind.springproject.space.repository.RoomOperatingPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 휴무일 설정 Application Service.
 * <p>
 * 룸의 휴무일을 설정하고, 이미 생성된 슬롯의 상태를 비동기로 업데이트한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClosedDateSetupApplicationService {

	private final RoomOperatingPolicyRepository policyRepository;
	private final ClosedDateUpdateRequestRepository updateRequestRepository;
	private final EventPublisher eventPublisher;

	/**
	 * 휴무일을 설정하고 슬롯 업데이트를 요청한다.
	 * <p>
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

		// 1. RoomOperatingPolicy 조회
		RoomOperatingPolicy policy = policyRepository.findByRoomId(request.getRoomId())
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

		policyRepository.save(policy);

		log.info("Closed dates added to policy: roomId={}, policyId={}, count={}",
				request.getRoomId(), policy.getPolicyId(), closedDateRanges.size());

		// 3. 요청 ID 생성
		String requestId = UUID.randomUUID().toString();

		// 4. 업데이트 요청을 DB에 저장
		ClosedDateUpdateRequest updateRequest = ClosedDateUpdateRequest.create(
				requestId,
				request.getRoomId(),
				request.getClosedDates().size()
		);
		updateRequestRepository.save(updateRequest);

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
