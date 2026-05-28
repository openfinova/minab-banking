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

import com.openfinova.banking.compliance.dto.OperatorNoteResponse;
import com.openfinova.banking.compliance.mapper.OperatorNoteMapper;
import com.openfinova.banking.compliance.service.OperatorNoteService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/notes")
public class OperatorNotesController {

    private final OperatorNoteService operatorNoteService;
    private final OperatorNoteMapper operatorNoteMapper;

    public OperatorNotesController(OperatorNoteService operatorNoteService, OperatorNoteMapper operatorNoteMapper) {
        this.operatorNoteService = operatorNoteService;
        this.operatorNoteMapper = operatorNoteMapper;
    }

    public record OperatorNoteBody(@NotBlank String entityType, @NotNull UUID entityId, @NotBlank String body) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('operator:note:read')")
    public java.util.List<OperatorNoteResponse> list(@RequestParam String entityType, @RequestParam UUID entityId) {
        return operatorNoteMapper.toResponseList(operatorNoteService.listNotes(entityType, entityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('operator:note:write')")
    public OperatorNoteResponse create(@RequestBody OperatorNoteBody body, Authentication authentication) {
        return operatorNoteMapper.toResponse(
                operatorNoteService.createNote(body.entityType(), body.entityId(), body.body(), authentication));
    }
}
