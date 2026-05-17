package com.openfinova.banking.compliance.web;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/screening/customers")
public class ScreeningStubController {

    @GetMapping("/{customerId}/runs")
    @PreAuthorize("hasAuthority('compliance:screening:read')")
    public Page<Map<String, Object>> list(@PathVariable UUID customerId, Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    @PostMapping("/{customerId}/runs")
    @PreAuthorize("hasAuthority('compliance:screening:run')")
    public Map<String, Object> run(@PathVariable UUID customerId, @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {
        Map<String, Object> out = new HashMap<>();
        out.put("runId", UUID.randomUUID().toString());
        out.put("customerId", customerId.toString());
        out.put("requestedBy", authentication != null ? authentication.getName() : "system");
        out.put("requestedAt", Instant.now().toString());
        out.put("payload", body);
        return out;
    }
}
