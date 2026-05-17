package com.openfinova.banking.compliance.web;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/me")
public class StaffNotificationsStubController {

    @GetMapping
    @PreAuthorize("hasAuthority('notification:read')")
    public Page<Map<String, Object>> inbox(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:write')")
    public void markRead(@PathVariable String id, @RequestBody(required = false) Map<String, Object> ignoredBody) {
        // Stub: staffing push service replaces this with durable rows (S6).
    }
}
