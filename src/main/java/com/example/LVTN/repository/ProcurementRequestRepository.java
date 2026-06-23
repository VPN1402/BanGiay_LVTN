package com.example.LVTN.repository;

import com.example.LVTN.entity.ProcurementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, Long> {
    List<ProcurementRequest> findByStatus(String status);
}