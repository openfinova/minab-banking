package com.openfinova.banking.compliance.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.compliance.entity.OperatorNote;
import com.openfinova.banking.compliance.repository.OperatorNoteRepository;

@Service
@Transactional
public class OperatorNoteService {

    private final OperatorNoteRepository noteRepository;

    public OperatorNoteService(OperatorNoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('operator:note:read')")
    public List<OperatorNote> listNotes(String entityType, UUID entityId) {
        return noteRepository.findByEntityTypeIgnoreCaseAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @PreAuthorize("hasAuthority('operator:note:write')")
    public OperatorNote createNote(String entityType, UUID entityId, String body, Authentication authentication) {
        OperatorNote note = new OperatorNote();
        note.setEntityType(entityType);
        note.setEntityId(entityId);
        note.setBody(body);
        note.setAuthorUsername(authentication != null ? authentication.getName() : "system");
        return noteRepository.save(note);
    }
}
