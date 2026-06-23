package com.example.LVTN.service;

import com.example.LVTN.entity.ProcurementRequest;

public interface ProcurementService {
    void generateRequestIfNeeded(String note);
    ProcurementRequest getDetailById(Long id);
}
