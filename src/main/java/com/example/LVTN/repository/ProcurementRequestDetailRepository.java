package com.example.LVTN.repository;


import com.example.LVTN.entity.ProcurementRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Thêm import này

import java.util.List;

@Repository // Thêm cái này để Spring nhận diện nó là Bean
public interface ProcurementRequestDetailRepository extends JpaRepository<ProcurementRequestDetail, Long> {

}