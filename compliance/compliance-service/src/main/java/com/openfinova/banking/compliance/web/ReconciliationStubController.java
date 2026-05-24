package com.openfinova.banking.compliance.web;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reconciliation/runs")
public class ReconciliationStubController {

    @GetMapping
    @PreAuthorize("hasAuthority('reconciliation:read')")
    public Page<Map<String, Object>> list(Pageable pageable) {
        return new PageImpl<>(java.util.List.of(), pageable, 0);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('reconciliation:write')")
    public Map<String, Object> open(@RequestBody Map<String, Object> body) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", UUID.randomUUID().toString());
        out.put("accepted", body);
        return out;
    }
}
