package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.dto.GLAuditTrailDTO;
import com.openfinova.banking.gl.entity.GLAuditTrail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting GLAuditTrail entity to GLAuditTrailDTO.
 */
@Component
public class GLAuditTrailMapper {

    /**
     * Convert GLAuditTrail entity to DTO.
     *
     * @param entity the audit trail entity
     * @return the audit trail DTO
     */
    public GLAuditTrailDTO toDTO(GLAuditTrail entity) {
        if (entity == null) {
            return null;
        }

        return new GLAuditTrailDTO(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getPerformedBy(),
                entity.getPerformedAt(),
                entity.getReason(),
                entity.getOldValues(),
                entity.getNewValues(),
                entity.getIpAddress(),
                entity.getSessionId(),
                entity.getTransactionAmount(),
                entity.getTransactionCurrency(),
                entity.getCorrelationId());
    }

    /**
     * Convert list of GLAuditTrail entities to list of DTOs.
     *
     * @param entities list of audit trail entities
     * @return list of audit trail DTOs
     */
    public List<GLAuditTrailDTO> toDTOList(List<GLAuditTrail> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }
}
