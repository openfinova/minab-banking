package com.openfinova.banking.identity.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.identity.dto.ApprovalWorkflowResponse;
import com.openfinova.banking.identity.dto.CreateApprovalWorkflowRequest;
import com.openfinova.banking.identity.dto.WorkflowActionRequest;
import com.openfinova.banking.identity.service.IdentityApprovalWorkflowService;
import com.openfinova.banking.identity.service.RoleManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/identity/approval-workflows")
@Tag(name = "Approval workflows", description = "Generic multi-step approval chains (identity module).")
@SecurityRequirement(name = "oauth2")
public class IdentityApprovalWorkflowController {

    private final IdentityApprovalWorkflowService workflowService;

    public IdentityApprovalWorkflowController(IdentityApprovalWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:doa:write')")
    @Operation(summary = "Start an approval workflow for a logical resource")
    public ResponseEntity<ApprovalWorkflowResponse> start(Authentication auth,
            @Valid @RequestBody CreateApprovalWorkflowRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApprovalWorkflowResponse.from(workflowService.start(request, actor)));
    }

    @GetMapping("/resource-types")
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "Known workflow resource types for UI pickers")
    public List<String> listApprovalWorkflowResourceTypes() {
        return List.of("USER_PROVISIONING", RoleManagementService.RESOURCE_TYPE_ROLE_PERMISSION_CHANGE);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "Get workflow with steps")
    public ApprovalWorkflowResponse get(@PathVariable UUID id) {
        return ApprovalWorkflowResponse.from(workflowService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:doa:read')")
    @Operation(summary = "List workflows by resource type")
    public List<ApprovalWorkflowResponse> listByType(@RequestParam String resourceType) {
        return workflowService.listByResourceType(resourceType).stream().map(ApprovalWorkflowResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('admin:doa:write') or hasAuthority('gl:approve')")
    @Operation(summary = "Approve the current pending step (actor must satisfy required GL tier)")
    public ApprovalWorkflowResponse approve(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody(required = false) WorkflowActionRequest body) {
        BankingPrincipal me = BankingPrincipal.from(auth);
        UUID actorUserId = me.userId();
        if (actorUserId == null) {
            throw new IllegalArgumentException("JWT subject must be a user id to approve workflow steps");
        }
        AuditActor actor = AuditActor.fromPrincipal(me);
        String comment = body != null ? body.getComment() : null;
        return ApprovalWorkflowResponse.from(workflowService.approve(id, actorUserId, comment, actor));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('admin:doa:write') or hasAuthority('gl:approve')")
    @Operation(summary = "Reject at the current pending step")
    public ApprovalWorkflowResponse reject(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody(required = false) WorkflowActionRequest body) {
        BankingPrincipal me = BankingPrincipal.from(auth);
        UUID actorUserId = me.userId();
        if (actorUserId == null) {
            throw new IllegalArgumentException("JWT subject must be a user id to reject workflow steps");
        }
        AuditActor actor = AuditActor.fromPrincipal(me);
        String reason = body != null ? body.getComment() : null;
        return ApprovalWorkflowResponse.from(workflowService.reject(id, actorUserId, reason, actor));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('admin:doa:write')")
    @Operation(summary = "Cancel an open workflow")
    public ApprovalWorkflowResponse cancel(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ApprovalWorkflowResponse.from(workflowService.cancel(id, actor));
    }
}
