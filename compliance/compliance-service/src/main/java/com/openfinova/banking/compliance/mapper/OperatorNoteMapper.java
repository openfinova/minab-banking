package com.openfinova.banking.compliance.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.openfinova.banking.compliance.dto.OperatorNoteResponse;
import com.openfinova.banking.compliance.entity.OperatorNote;

@Component
public class OperatorNoteMapper {

    public OperatorNoteResponse toResponse(OperatorNote note) {
        if (note == null) {
            return null;
        }
        OperatorNoteResponse response = new OperatorNoteResponse();
        response.setId(note.getId());
        response.setEntityType(note.getEntityType());
        response.setEntityId(note.getEntityId());
        response.setAuthorUsername(note.getAuthorUsername());
        response.setBody(note.getBody());
        response.setCreatedAt(note.getCreatedAt());
        return response;
    }

    public List<OperatorNoteResponse> toResponseList(List<OperatorNote> notes) {
        if (notes == null) {
            return List.of();
        }
        return notes.stream().map(this::toResponse).toList();
    }
}
