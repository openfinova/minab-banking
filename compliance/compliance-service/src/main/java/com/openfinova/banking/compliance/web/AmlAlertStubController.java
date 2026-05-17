package com.openfinova.banking.compliance.web;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aml/alerts")
public class AmlAlertStubController {

    @GetMapping
    @PreAuthorize("hasAuthority('compliance:alert:read')")
    public Page<Map<String, Object>> list(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }
}
