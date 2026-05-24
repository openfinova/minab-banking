package com.openfinova.banking.compliance.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.compliance.entity.AmlAlert;

public interface AmlAlertRepository extends JpaRepository<AmlAlert, UUID> {

    Page<AmlAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
