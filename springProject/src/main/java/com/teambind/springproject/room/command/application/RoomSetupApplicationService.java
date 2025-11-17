package com.teambind.springproject.room.command.application;

import com.teambind.springproject.common.exceptions.domain.RequestNotFoundException;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.dto.RoomOperatingPolicySetupRequest;
import com.teambind.springproject.room.command.dto.WeeklySlotDto;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.domain.port.SlotGenerationRequestPort;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.SlotGenerationRequest;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.entity.vo.WeeklySlotTime;
import com.teambind.springproject.room.event.event.SlotGenerationRequestedEvent;
import com.teambind.springproject.room.query.dto.RoomSetupResponse;
import com.teambind.springproject.room.query.dto.SlotGenerationStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 룸 초기 설정 Application Service.
 * <p>
 * 룸의 초기 데이터 셋업을 담당한다.
 * 운영 정책 저장 후 비동기로 슬롯을 생성한다.
 * <p>
 * Hexagonal Architecture 적용:
 * <p>
 * Infrastructure 계층(JPA)에 직접 의존하지 않고 Port 인터페이스에 의존
 * DIP (Dependency Inversion Principle) 준수
 * Use Case 조율만 담당 (비즈니스 로직은 DomainService/Entity에 위임)
 *
 */
@Slf4j
@Service
public class RoomSetupApplicationService {
	
	private final OperatingPolicyPort operatingPolicyPort;
	private final SlotGenerationRequestPort slotGenerationRequestPort;
	private final EventPublisher eventPublisher;

	@Value("${room.timeSlot.rollingWindow.days:30}")
	private int rollingWindowDays;

	public RoomSetupApplicationService(
			OperatingPolicyPort operatingPolicyPort,
			SlotGenerationRequestPort slotGenerationRequestPort,
			EventPublisher eventPublisher
	) {
		this.operatingPolicyPort = operatingPolicyPort;
		this.slotGenerationRequestPort = slotGenerationRequestPort;
		this.eventPublisher = eventPublisher;
	}
	
	/**
	 * 룸 운영 정책을 설정하고 슬롯 생성을 요청한다.
	 * <p>
	 * 플로우:
	 * 1. 운영 정책을 RoomOperatingPolicy에 저장
	 * 2. 슬롯 생성 요청을 DB에 저장 (상태: REQUESTED)
	 * 3. Kafka 이벤트 발행
	 * 4. 즉시 응답 반환 (202 Accepted)
	 *
	 * @param request 운영 정책 설정 요청
	 * @return 설정 응답 (요청 ID 포함)
	 */
	@Transactional
	public RoomSetupResponse setupRoom(RoomOperatingPolicySetupRequest request) {
		// 1. RecurrencePattern 추출 (첫 번째 슬롯의 패턴 사용)
		RecurrencePattern recurrencePattern = request.getSlots().isEmpty()
				? RecurrencePattern.EVERY_WEEK
				: request.getSlots().get(0).getRecurrencePattern();
		
		log.info("Room operating policy setup requested: roomId={}, recurrence={}",
				request.getRoomId(), recurrencePattern);
		
		// 2. WeeklySlotSchedule 생성
		List<WeeklySlotTime> slotTimes = new ArrayList<>();
		for (WeeklySlotDto slotDto : request.getSlots()) {
			for (LocalTime startTime : slotDto.getStartTimes()) {
				slotTimes.add(WeeklySlotTime.of(slotDto.getDayOfWeek(), startTime));
			}
		}
		WeeklySlotSchedule weeklySchedule = WeeklySlotSchedule.of(slotTimes);
		
		// 3. RoomOperatingPolicy 생성 및 저장 (Port 사용)
		RoomOperatingPolicy policy = RoomOperatingPolicy.create(
				request.getRoomId(),
				weeklySchedule,
				recurrencePattern,
				Collections.emptyList() // 초기 설정 시 휴무일 없음
		);
		operatingPolicyPort.save(policy);
		
		log.info("Room operating policy saved: roomId={}, policyId={}",
				request.getRoomId(), policy.getPolicyId());

		// 3. 슬롯 생성 날짜 범위 계산 (오늘부터 N일, 설정값 사용)
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusDays(rollingWindowDays);

		log.debug("Slot generation date range calculated: startDate={}, endDate={}, rollingWindowDays={}",
				startDate, endDate, rollingWindowDays);
		
		// 4. 요청 ID 생성
		String requestId = UUID.randomUUID().toString();
		
		// 5. 슬롯 생성 요청을 DB에 저장 (Port 사용)
		SlotGenerationRequest generationRequest = SlotGenerationRequest.create(
				requestId,
				request.getRoomId(),
				startDate,
				endDate
		);
		slotGenerationRequestPort.save(generationRequest);
		
		log.info("Slot generation request saved: requestId={}, dateRange={} to {}",
				requestId, startDate, endDate);
		
		// 6. Kafka 이벤트 발행 (roomId만 전달, 정책은 이미 저장됨)
		SlotGenerationRequestedEvent event = SlotGenerationRequestedEvent.of(
				requestId,
				request.getRoomId(),
				startDate,
				endDate
		);
		eventPublisher.publish(event);
		
		log.info("SlotGenerationRequestedEvent published: requestId={}", requestId);
		
		// 7. 응답 생성
		return new RoomSetupResponse(
				generationRequest.getRequestId(),
				generationRequest.getRoomId(),
				generationRequest.getStartDate(),
				generationRequest.getEndDate(),
				generationRequest.getStatus(),
				generationRequest.getRequestedAt()
		);
	}
	
	/**
	 * 슬롯 생성 상태를 조회한다.
	 *
	 * @param requestId 요청 ID
	 * @return 상태 응답
	 */
	@Transactional(readOnly = true)
	public SlotGenerationStatusResponse getStatus(String requestId) {
		log.info("Querying slot generation status: requestId={}", requestId);
		
		SlotGenerationRequest request = slotGenerationRequestPort.findById(requestId)
				.orElseThrow(() -> new RequestNotFoundException(
						"Slot generation request not found: " + requestId
				));
		
		return SlotGenerationStatusResponse.from(request);
	}
}
