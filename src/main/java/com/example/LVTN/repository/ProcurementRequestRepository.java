package com.example.LVTN.repository;

import com.example.LVTN.entity.ProcurementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, Long> {
}