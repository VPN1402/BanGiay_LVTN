package com.example.LVTN.repository;

import com.example.LVTN.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop20ByOrderByCreatedAtDesc();

    List<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

}