package com.teambind.springproject.room.repository;

import com.teambind.springproject.room.entity.SlotGenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SlotGenerationRequest 리포지토리.
 */
public interface SlotGenerationRequestRepository extends JpaRepository<SlotGenerationRequest, String> {
}