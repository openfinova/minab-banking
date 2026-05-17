package com.openfinova.banking.compliance.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.compliance.entity.OperatorNote;

public interface OperatorNoteRepository extends JpaRepository<OperatorNote, UUID> {

    List<OperatorNote> findByEntityTypeIgnoreCaseAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
