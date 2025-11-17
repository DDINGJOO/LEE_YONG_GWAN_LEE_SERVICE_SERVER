package com.teambind.springproject.room.controller;

import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.repository.RoomOperatingPolicyRepository;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import com.teambind.springproject.room.repository.SlotGenerationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 룸 초기 설정 통합 테스트.
 * <p>
 * 테스트 범위:
 * - API 요청 → 운영 정책 저장 → 이벤트 발행 → 슬롯 생성
 * - 전체 플로우 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoomSetupIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RoomOperatingPolicyRepository policyRepository;

	@Autowired
	private SlotGenerationRequestRepository generationRequestRepository;

	@Autowired
	private RoomTimeSlotRepository timeSlotRepository;

	@Value("${room.timeSlot.rollingWindow.days:30}")
	private int rollingWindowDays;

	private Long testRoomId;

	@BeforeEach
	void setUp() {
		testRoomId = 101L;

		// 기존 데이터 정리
		timeSlotRepository.deleteByRoomId(testRoomId);
		policyRepository.deleteByRoomId(testRoomId);
	}

	@Test
	@DisplayName("운영시간 입력 시 운영 정책이 저장되고 슬롯 생성 요청이 생성된다")
	void setupRoom_CreatesPolicy_AndGenerationRequest() throws Exception {
		// Given: 운영 정책 설정 요청
		String requestBody = """
				{
				  "roomId": 101,
				  "slots": [
				    {
				      "dayOfWeek": "MONDAY",
				      "startTimes": ["09:00", "10:00", "11:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    },
				    {
				      "dayOfWeek": "TUESDAY",
				      "startTimes": ["14:00", "15:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    }
				  ]
				}
				""";

		// When: POST /api/rooms/setup
		String responseBody = mockMvc.perform(post("/api/rooms/setup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andDo(print())
				.andExpect(status().isAccepted()) // 202 Accepted
				.andExpect(jsonPath("$.requestId").exists())
				.andExpect(jsonPath("$.roomId").value(101))
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		// Then: 운영 정책이 저장되었는지 확인
		RoomOperatingPolicy policy = policyRepository.findByRoomId(testRoomId)
				.orElseThrow(() -> new AssertionError("Policy not found"));

		assertThat(policy.getRoomId()).isEqualTo(testRoomId);
		assertThat(policy.getWeeklySchedule().getSlotTimes()).hasSize(5); // 3 + 2
	}

	@Test
	@DisplayName("슬롯 생성 상태를 조회할 수 있다")
	void getStatus_ReturnsGenerationStatus() throws Exception {
		// Given: 운영 정책과 슬롯 생성 요청 저장
		String requestBody = """
				{
				  "roomId": 101,
				  "slots": [
				    {
				      "dayOfWeek": "MONDAY",
				      "startTimes": ["09:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    }
				  ]
				}
				""";

		String setupResponse = mockMvc.perform(post("/api/rooms/setup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String requestId = setupResponse.split("\"requestId\":\"")[1].split("\"")[0];

		// When: GET /api/rooms/setup/{requestId}/status
		mockMvc.perform(get("/api/rooms/setup/" + requestId + "/status"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requestId").value(requestId))
				.andExpect(jsonPath("$.roomId").value(101))
				.andExpect(jsonPath("$.status").value("REQUESTED"));
	}

	@Test
	@DisplayName("Rolling Window 설정값이 적용되어 슬롯 생성 요청이 생성된다")
	void setupRoom_UsesRollingWindowConfiguration() throws Exception {
		// Given
		String requestBody = """
				{
				  "roomId": 101,
				  "slots": [
				    {
				      "dayOfWeek": "MONDAY",
				      "startTimes": ["09:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    }
				  ]
				}
				""";

		// When & Then
		mockMvc.perform(post("/api/rooms/setup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.startDate").exists())
				.andExpect(jsonPath("$.endDate").exists())
				.andExpect(jsonPath("$.requestId").exists())
				.andExpect(jsonPath("$.status").value("REQUESTED"));
	}

	@Test
	@DisplayName("여러 요일의 운영시간을 동시에 설정할 수 있다")
	void setupRoom_MultipleWeekdays_Success() throws Exception {
		// Given: 5개 요일 설정
		String requestBody = """
				{
				  "roomId": 101,
				  "slots": [
				    {
				      "dayOfWeek": "MONDAY",
				      "startTimes": ["09:00", "10:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    },
				    {
				      "dayOfWeek": "TUESDAY",
				      "startTimes": ["14:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    },
				    {
				      "dayOfWeek": "WEDNESDAY",
				      "startTimes": ["09:00", "11:00", "13:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    },
				    {
				      "dayOfWeek": "THURSDAY",
				      "startTimes": ["10:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    },
				    {
				      "dayOfWeek": "FRIDAY",
				      "startTimes": ["15:00", "16:00", "17:00"],
				      "recurrencePattern": "EVERY_WEEK"
				    }
				  ]
				}
				""";

		// When
		mockMvc.perform(post("/api/rooms/setup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isAccepted());

		// Then: 총 10개의 슬롯 시간이 저장됨
		RoomOperatingPolicy policy = policyRepository.findByRoomId(testRoomId)
				.orElseThrow(() -> new AssertionError("Policy not found"));

		assertThat(policy.getWeeklySchedule().getSlotTimes()).hasSize(10); // 2+1+3+1+3
	}

}
