package com.teambind.springproject.space.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 슬롯 생성 요청 이벤트.
 * <p>
 * 비동기로 슬롯 생성을 시작하기 위한 이벤트.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotGenerationRequestedEvent extends Event {

	private static final String TOPIC = "slot-generation-requested";
	private static final String EVENT_TYPE = "SlotGenerationRequested";

	private String requestId;
	private Long roomId;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalDateTime requestedAt;

	private SlotGenerationRequestedEvent(
			String requestId,
			Long roomId,
			LocalDate startDate,
			LocalDate endDate,
			LocalDateTime requestedAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.requestId = requestId;
		this.roomId = roomId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.requestedAt = requestedAt;
	}

	public static SlotGenerationRequestedEvent of(
			String requestId,
			Long roomId,
			LocalDate startDate,
			LocalDate endDate
	) {
		return new SlotGenerationRequestedEvent(
				requestId,
				roomId,
				startDate,
				endDate,
				LocalDateTime.now()
		);
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}