package com.teambind.springproject.space.scheduler;

import com.teambind.springproject.space.service.TimeSlotGenerationService;
import com.teambind.springproject.space.service.TimeSlotManagementService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 시간 슬롯 관리 스케줄러.
 *
 * 주요 책임:
 *
 *
 *   매일 Rolling Window 유지 (어제 슬롯 삭제, 60일 후 슬롯 생성)
 *   만료된 PENDING 슬롯 복구
 *
 *
 * 분산 환경 고려:
 *
 *
 *   ShedLock을 사용하여 중복 실행 방지
 *   lockAtMostFor: 작업 실패 시 자동 Lock 해제
 *   lockAtLeastFor: 최소 실행 간격 보장
 *
 */
@Component
public class TimeSlotScheduler {
	
	private static final Logger log = LoggerFactory.getLogger(TimeSlotScheduler.class);
	
	private final TimeSlotGenerationService generationService;
	private final TimeSlotManagementService managementService;
	
	public TimeSlotScheduler(
			TimeSlotGenerationService generationService, TimeSlotManagementService managementService) {
		this.generationService = generationService;
		this.managementService = managementService;
	}
	
	/**
	 * 매일 새벽 2시에 Rolling Window를 유지한다.
	 *
	 * <p>처리 플로우:
	 *
	 * <ol>
	 *   <li>어제 날짜의 슬롯 삭제
	 *   <li>60일 후 날짜의 슬롯 생성
	 * </ol>
	 *
	 * <p>Lock 설정:
	 *
	 * <ul>
	 *   <li>lockAtMostFor: 5분 (작업이 5분 이상 걸리면 자동 해제)
	 *   <li>lockAtLeastFor: 1분 (최소 1분 간격 유지)
	 * </ul>
	 */
	@Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
	@SchedulerLock(
			name = "maintainRollingWindow",
			lockAtMostFor = "PT5M", // ISO-8601 Duration: 5분
			lockAtLeastFor = "PT1M") // 최소 1분 간격
	public void maintainRollingWindow() {
		log.info("Starting rolling window maintenance");
		
		try {
			// 1. 어제 슬롯 삭제
			int deletedCount = generationService.deleteYesterdaySlots();
			log.info("Deleted yesterday's slots: count={}", deletedCount);
			
			// 2. 60일 후 슬롯 생성
			LocalDate targetDate = LocalDate.now().plusDays(60);
			int createdCount = generationService.generateSlotsForAllRooms(targetDate);
			log.info("Created slots for date {}: count={}", targetDate, createdCount);
			
			log.info("Rolling window maintenance completed: deleted={}, created={}",
					deletedCount, createdCount);
		} catch (Exception e) {
			log.error("Failed to maintain rolling window", e);
			throw e; // 재시도를 위해 예외 전파
		}
	}
	
	/**
	 * 5분마다 만료된 PENDING 슬롯을 복구한다.
	 *
	 * <p>15분 이상 PENDING 상태인 슬롯을 AVAILABLE로 복구한다.
	 *
	 * <p>Lock 설정:
	 *
	 * <ul>
	 *   <li>lockAtMostFor: 2분 (작업이 2분 이상 걸리면 자동 해제)
	 *   <li>lockAtLeastFor: 30초 (최소 30초 간격 유지)
	 * </ul>
	 */
	@Scheduled(fixedDelay = 300000) // 5분마다
	@SchedulerLock(
			name = "restoreExpiredPendingSlots",
			lockAtMostFor = "PT2M", // 2분
			lockAtLeastFor = "PT30S") // 최소 30초 간격
	public void restoreExpiredPendingSlots() {
		try {
			int restoredCount = managementService.restoreExpiredPendingSlots();
			
			if (restoredCount > 0) {
				log.info("Restored expired pending slots: count={}", restoredCount);
			} else {
				log.debug("No expired pending slots to restore");
			}
		} catch (Exception e) {
			log.error("Failed to restore expired pending slots", e);
			throw e;
		}
	}
}
