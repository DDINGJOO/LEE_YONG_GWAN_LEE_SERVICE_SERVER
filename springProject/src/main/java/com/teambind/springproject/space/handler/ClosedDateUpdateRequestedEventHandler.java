package com.teambind.springproject.space.handler;

import com.teambind.springproject.common.exceptions.domain.PolicyNotFoundException;
import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.entity.ClosedDateUpdateRequest;
import com.teambind.springproject.space.entity.RoomOperatingPolicy;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.entity.vo.ClosedDateRange;
import com.teambind.springproject.space.event.ClosedDateUpdateRequestedEvent;
import com.teambind.springproject.space.repository.ClosedDateUpdateRequestRepository;
import com.teambind.springproject.space.repository.RoomOperatingPolicyRepository;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 휴무일 업데이트 요청 이벤트 핸들러.
 * <p>
 * 비동기로 기존 슬롯의 상태를 CLOSED로 변경한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClosedDateUpdateRequestedEventHandler implements EventHandler<ClosedDateUpdateRequestedEvent> {

	private final ClosedDateUpdateRequestRepository updateRequestRepository;
	private final RoomOperatingPolicyRepository policyRepository;
	private final RoomTimeSlotRepository slotRepository;

	@Override
	@Transactional
	public void handle(ClosedDateUpdateRequestedEvent event) {
		log.info("Processing ClosedDateUpdateRequestedEvent: requestId={}, roomId={}",
				event.getRequestId(), event.getRoomId());

		// 1. 요청 조회
		ClosedDateUpdateRequest request = updateRequestRepository.findById(event.getRequestId())
				.orElseThrow(() -> new IllegalStateException(
						"ClosedDateUpdateRequest not found: " + event.getRequestId()
				));

		try {
			// 2. 처리 시작 상태로 변경
			request.markAsInProgress();
			updateRequestRepository.save(request);

			log.info("Starting closed date update: requestId={}", event.getRequestId());

			// 3. RoomOperatingPolicy에서 휴무일 조회
			RoomOperatingPolicy policy = policyRepository.findByRoomId(event.getRoomId())
					.orElseThrow(() -> new PolicyNotFoundException(event.getRoomId(), true));

			List<ClosedDateRange> closedDateRanges = policy.getClosedDates();

			log.info("Found {} closed date ranges for roomId={}",
					closedDateRanges.size(), event.getRoomId());

			int affectedSlots = 0;

			// 4. 각 휴무일 범위에 대해 슬롯 업데이트
			for (ClosedDateRange range : closedDateRanges) {
				List<RoomTimeSlot> slots;

				if (range.isPatternBased()) {
					// 패턴 기반 휴무일: 모든 슬롯을 조회하여 매칭되는 것만 필터링
					// (60일치 범위에서 해당 요일/패턴에 맞는 슬롯 찾기)
					LocalDate today = LocalDate.now();
					LocalDate futureDate = today.plusMonths(2);

					slots = slotRepository.findByRoomIdAndSlotDateBetween(
							event.getRoomId(),
							today,
							futureDate
					);

					log.debug("Pattern-based closed date: dayOfWeek={}, pattern={}, found {} slots in range",
							range.getDayOfWeek(), range.getRecurrencePattern(), slots.size());
				} else {
					// 날짜 기반 휴무일: 해당 날짜 범위의 슬롯만 조회
					LocalDate startDate = range.getStartDate();
					LocalDate endDate = range.getEndDate() != null ? range.getEndDate() : startDate;

					slots = slotRepository.findByRoomIdAndSlotDateBetween(
							event.getRoomId(),
							startDate,
							endDate
					);

					log.debug("Date-based closed date: {} to {}, found {} slots",
							startDate, endDate, slots.size());
				}

				// 휴무 범위에 해당하는 슬롯만 CLOSED로 변경
				for (RoomTimeSlot slot : slots) {
					if (range.contains(slot.getSlotDate(), slot.getSlotTime())) {
						try {
							slot.markAsClosed();
							slotRepository.save(slot);
							affectedSlots++;
						} catch (Exception e) {
							// 이미 예약된 슬롯 등 상태 전환이 불가능한 경우 로그만 남기고 계속 진행
							log.warn("Failed to mark slot as closed: slotId={}, status={}, reason={}",
									slot.getSlotId(), slot.getStatus(), e.getMessage());
						}
					}
				}
			}

			// 5. 완료 상태로 변경
			request.markAsCompleted(affectedSlots);
			updateRequestRepository.save(request);

			log.info("Closed date update completed: requestId={}, affectedSlots={}",
					event.getRequestId(), affectedSlots);

		} catch (Exception e) {
			// 6. 실패 상태로 변경
			log.error("Closed date update failed: requestId={}", event.getRequestId(), e);

			request.markAsFailed(e.getMessage());
			updateRequestRepository.save(request);

			// 예외를 다시 던지지 않음 - DLQ로 이동하지 않고 상태만 업데이트
		}
	}

	@Override
	public String getSupportedEventType() {
		return "ClosedDateUpdateRequested";
	}
}
