package com.teambind.springproject.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * JPA 엔티티에서 Spring 이벤트를 발행하기 위한 유틸리티.
 * <p>
 * JPA Lifecycle Callback(@PostPersist, @PreRemove 등)에서는
 * 직접 빈을 주입받을 수 없으므로, 정적 메서드를 통해 이벤트를 발행합니다.
 * <p>
 * 사용 예:
 * <pre>
 * {@code
 * @PostPersist
 * private void publishEvent() {
 *     DomainEventPublisher.publish(new MyDomainEvent(...));
 * }
 * }
 * </pre>
 */
@Slf4j
@Component
public class DomainEventPublisher {

	private static ApplicationEventPublisher eventPublisher;

	/**
	 * Spring이 생성자 주입을 통해 ApplicationEventPublisher를 설정합니다.
	 *
	 * @param publisher ApplicationEventPublisher
	 */
	public DomainEventPublisher(ApplicationEventPublisher publisher) {
		DomainEventPublisher.eventPublisher = publisher;
		log.debug("DomainEventPublisher initialized");
	}

	/**
	 * 도메인 이벤트를 발행합니다.
	 * <p>
	 * JPA Lifecycle Callback에서 호출되어 Spring의 이벤트 시스템으로 전달합니다.
	 *
	 * @param event 발행할 이벤트
	 */
	public static void publish(Object event) {
		if (eventPublisher == null) {
			log.warn("DomainEventPublisher not initialized, event not published: {}",
					event.getClass().getSimpleName());
			return;
		}

		eventPublisher.publishEvent(event);
		log.debug("Domain event published: {}", event.getClass().getSimpleName());
	}
}