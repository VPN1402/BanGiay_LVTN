package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Supplier;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    public List<Supplier> findAll() {

        return supplierRepository.findAll();
    }

    @Override
    public Supplier findById(Long id) {

        return supplierRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void save(Supplier supplier) {
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        supplierRepository.deleteById(id);
    }

    @Override
    public boolean hasReceipts(Long id) {
        Supplier supplier = findById(id);
        if (supplier != null && supplier.getImportReceipts() != null) {
            return !supplier.getImportReceipts().isEmpty();
        }
        return false;
    }
}