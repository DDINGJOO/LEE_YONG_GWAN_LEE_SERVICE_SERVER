package com.teambind.springproject.space.repository;

import com.teambind.springproject.space.entity.SlotGenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SlotGenerationRequest 리포지토리.
 */
public interface SlotGenerationRequestRepository extends JpaRepository<SlotGenerationRequest, String> {
}