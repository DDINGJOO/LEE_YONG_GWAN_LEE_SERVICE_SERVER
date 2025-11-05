package com.teambind.springproject.room.repository;

import com.teambind.springproject.room.entity.ClosedDateUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ClosedDateUpdateRequest에 대한 데이터 접근 계층.
 */
@Repository
public interface ClosedDateUpdateRequestRepository extends JpaRepository<ClosedDateUpdateRequest, String> {
}
