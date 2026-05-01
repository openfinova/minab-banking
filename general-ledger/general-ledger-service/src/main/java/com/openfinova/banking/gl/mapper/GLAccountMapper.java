package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.dto.GLAccountResponse;
import com.openfinova.banking.gl.api.dto.UpdateGLAccountRequest;
import com.openfinova.banking.gl.entity.GLAccount;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between GLAccount entities and DTOs.
 */
@Component
public class GLAccountMapper {

    /**
     * Converts a GLAccount entity to a response DTO.
     *
     * @param account the entity
     * @return the response DTO
     */
    public GLAccountResponse toResponse(GLAccount account) {
        if (account == null) {
            return null;
        }

        GLAccountResponse response = new GLAccountResponse();
        response.setId(account.getId());
        response.setCode(account.getCode());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setCurrency(account.getCurrency());
        response.setStatus(account.getStatus());
        response.setNormalBalance(account.getNormalBalance());
        response.setContra(account.isContra());
        response.setDescription(account.getDescription());
        response.setMetadata(account.getMetadata());
        response.setCreatedAt(account.getCreatedAt());
        response.setCreatedBy(account.getCreatedBy());
        response.setHasChildren(account.hasChildren());

        // Map parent information if present
        if (account.getParent() != null) {
            response.setParentId(account.getParent().getId());
            response.setParentCode(account.getParent().getCode());
            response.setParentName(account.getParent().getName());
        }

        return response;
    }

    /**
     * Updates an existing GLAccount entity from an update request DTO.
     *
     * @param account the entity to update
     * @param request the update request
     */
    public void updateEntityFromRequest(GLAccount account, UpdateGLAccountRequest request) {
        if (account == null || request == null) {
            return;
        }

        account.setName(request.getName());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency());
        account.setContra(request.isContra()); // automatically flips normalBalance via setContra
        account.setDescription(request.getDescription());
        account.setMetadata(request.getMetadata());
        // Note: parent is handled separately in the service layer
    }
}
