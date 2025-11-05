package com.teambind.springproject.room.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 휴무일 업데이트 요청 이벤트.
 * <p>
 * RoomOperatingPolicy에 휴무일이 추가되었으며, 기존 슬롯의 상태를 CLOSED로 변경해야 함을 알린다.
 * <p>
 * 경량 이벤트: 휴무일 데이터는 이벤트에 포함하지 않고, 핸들러가 DB에서 직접 조회한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClosedDateUpdateRequestedEvent extends Event {

	private static final String TOPIC = "closed-date-update-requested";
	private static final String EVENT_TYPE = "ClosedDateUpdateRequested";

	private String requestId;
	private Long roomId;
	private LocalDateTime requestedAt;

	private ClosedDateUpdateRequestedEvent(
			String requestId,
			Long roomId
	) {
		super(TOPIC, EVENT_TYPE);
		this.requestId = requestId;
		this.roomId = roomId;
		this.requestedAt = LocalDateTime.now();
	}

	/**
	 * ClosedDateUpdateRequestedEvent 인스턴스를 생성한다.
	 *
	 * @param requestId 요청 ID
	 * @param roomId    룸 ID
	 * @return 생성된 이벤트
	 */
	public static ClosedDateUpdateRequestedEvent of(
			String requestId,
			Long roomId
	) {
		return new ClosedDateUpdateRequestedEvent(requestId, roomId);
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
