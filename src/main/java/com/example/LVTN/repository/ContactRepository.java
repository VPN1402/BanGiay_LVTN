package com.example.LVTN.repository;

import com.example.LVTN.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    long countByStatus(Integer status);
}