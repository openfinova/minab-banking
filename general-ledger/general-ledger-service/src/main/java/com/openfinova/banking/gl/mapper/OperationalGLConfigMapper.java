package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.dto.OperationalGLConfigResponse;
import com.openfinova.banking.gl.entity.OperationalGLConfig;
import com.openfinova.banking.gl.service.GLAccountService;
import org.springframework.stereotype.Component;

@Component
public class OperationalGLConfigMapper {

    private final GLAccountService accountService;

    public OperationalGLConfigMapper(GLAccountService accountService) {
        this.accountService = accountService;
    }

    public OperationalGLConfigResponse toResponse(OperationalGLConfig config) {
        if (config == null) {
            return null;
        }

        OperationalGLConfigResponse response = new OperationalGLConfigResponse();
        response.setId(config.getId());
        response.setType(config.getConfigType());
        response.setGlAccountId(config.getGlAccountId());

        // Fetch account details
        accountService.getAccountById(config.getGlAccountId()).ifPresent(account -> {
            response.setGlAccountCode(account.getCode());
            response.setGlAccountName(account.getName());
        });

        response.setActive(config.isActive());
        response.setCreatedBy(config.getCreatedBy());
        response.setCreatedAt(config.getCreatedAt());

        return response;
    }
}
