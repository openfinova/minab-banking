package com.openfinova.banking.compliance.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.compliance.entity.OperatorNote;
import com.openfinova.banking.compliance.repository.OperatorNoteRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/notes")
public class OperatorNotesController {

    private final OperatorNoteRepository notes;

    public OperatorNotesController(OperatorNoteRepository notes) {
        this.notes = notes;
    }

    public record OperatorNoteBody(@NotBlank String entityType, @NotNull UUID entityId, @NotBlank String body) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('operator:note:read')")
    public java.util.List<OperatorNote> list(@RequestParam String entityType, @RequestParam UUID entityId) {
        return notes.findByEntityTypeIgnoreCaseAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('operator:note:write')")
    public OperatorNote create(@RequestBody OperatorNoteBody body, Authentication authentication) {
        OperatorNote n = new OperatorNote();
        n.setEntityType(body.entityType());
        n.setEntityId(body.entityId());
        n.setBody(body.body());
        n.setAuthorUsername(authentication != null ? authentication.getName() : "system");
        return notes.save(n);
    }
}
