package com.openfinova.banking.gl.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.GlApproveTransactionRequest;
import com.openfinova.banking.gl.api.dto.GlRejectTransactionRequest;
import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.dto.ApprovalResponse;
import com.openfinova.banking.gl.dto.AuthorizationLimitResponse;
import com.openfinova.banking.gl.dto.CanApproveResponse;
import com.openfinova.banking.gl.dto.PendingApprovalResponse;
import com.openfinova.banking.gl.service.ApprovalService;
import com.openfinova.banking.gl.service.GLTransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for GL transaction approval workflow operations.
 *
 * All endpoints require the {@code gl:approve} permission.
 * Username and GL approval role are resolved from the validated JWT rather than
 * being passed as request parameters, so they cannot be spoofed by callers.
 */
@RestController
@RequestMapping("/api/v1/gl/approvals")
@Tag(name = "GL Approval Workflow", description = "APIs for managing GL transaction approvals and maker-checker workflow")
@PreAuthorize("hasAuthority('gl:approve')")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    /** JWT claim that carries the GL approval role name (e.g. "MANAGER"). */
    private static final String CLAIM_GL_APPROVAL_ROLE = "gl_approval_role";

    private final ApprovalService approvalService;
    private final GLTransactionService glTransactionService;

    public ApprovalController(ApprovalService approvalService, GLTransactionService glTransactionService) {
        this.approvalService = approvalService;
        this.glTransactionService = glTransactionService;
    }

    @GetMapping("/my-queue")
    @Operation(summary = "Get my pending approvals queue", description = "Retrieves all transactions pending approval that the current user can approve "
            + "based on their GL approval role and authorization limits.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Pending approvals retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PendingApprovalResponse.class)))))
    public ResponseEntity<List<PendingApprovalResponse>> getMyApprovalQueue(Authentication auth) {
        String username = auth.getName();
        GLApprovalRole role = resolveGlRole(auth);
        log.info("GET /api/gl/approvals/my-queue  user={} role={}", username, role);
        return ResponseEntity.ok(approvalService.getPendingApprovalsForUser(username, role));
    }

    @GetMapping("/my-activity")
    @Operation(summary = "Get my approval activity history", description = "Retrieves the approval activity history for the currently authenticated user.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Approval activity retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApprovalResponse.class)))))
    public ResponseEntity<List<ApprovalResponse>> getMyApprovalActivity(Authentication auth) {
        String username = auth.getName();
        log.info("GET /api/gl/approvals/my-activity  user={}", username);
        return ResponseEntity.ok(approvalService.getApprovalActivityForUser(username));
    }

    @GetMapping("/my-limits")
    @Operation(summary = "Get my authorization limits", description = "Retrieves the authorization limits for the current user's GL approval role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization limits retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthorizationLimitResponse.class)))),
            @ApiResponse(responseCode = "404", description = "No authorization limits found for user role") })
    public ResponseEntity<List<AuthorizationLimitResponse>> getMyAuthorizationLimits(Authentication auth) {
        GLApprovalRole role = resolveGlRole(auth);
        log.info("GET /api/gl/approvals/my-limits  role={}", role);
        List<AuthorizationLimitResponse> limits = approvalService.getAuthorizationLimitsForRole(role);
        if (limits.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(limits);
    }

    @GetMapping("/{transactionId}/can-approve")
    @Operation(summary = "Check if I can approve a transaction", description = "Checks whether the current user can approve a specific transaction based on "
            + "their GL approval role and authorization limits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval check completed", content = @Content(schema = @Schema(implementation = CanApproveResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<CanApproveResponse> canApproveTransaction(
            @Parameter(description = "Transaction ID to check", required = true) @PathVariable UUID transactionId,
            Authentication auth) {
        String username = auth.getName();
        GLApprovalRole role = resolveGlRole(auth);
        log.info("GET /api/gl/approvals/{}/can-approve  user={} role={}", transactionId, username, role);
        return approvalService.checkCanApprove(transactionId, username, role).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{transactionId}/approve")
    @Operation(summary = "Approve pending GL transaction", description = "Records your approval. "
            + "When all required approval levels are satisfied, the transaction is posted. "
            + "Approver identity comes from the JWT — not the request body.")
    public ResponseEntity<java.util.Map<String, Object>> approveTransaction(
            @Parameter(description = "GL transaction id", required = true) @PathVariable UUID transactionId,
            Authentication auth, HttpServletRequest httpRequest,
            @RequestBody(required = false) @Valid GlApproveTransactionRequest body) {
        String username = auth.getName();
        GLApprovalRole role = resolveGlRole(auth);
        String comments = body != null ? body.getComments() : null;
        String ip = clientIp(httpRequest);
        boolean posted = glTransactionService.approveAndPostTransaction(transactionId, username, role, comments, ip);
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("posted", posted);
        payload.put("transactionId", transactionId);
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/{transactionId}/reject")
    @Operation(summary = "Reject pending GL transaction", description = "Rejects the transaction so it will not post. "
            + "Rejecter identity comes from the JWT — not spoofable via the body (only the reason is supplied).")
    public ResponseEntity<java.util.Map<String, Object>> rejectTransaction(
            @Parameter(description = "GL transaction id", required = true) @PathVariable UUID transactionId,
            Authentication auth, HttpServletRequest httpRequest, @Valid @RequestBody GlRejectTransactionRequest body) {
        String username = auth.getName();
        GLApprovalRole role = resolveGlRole(auth);
        String ip = clientIp(httpRequest);
        glTransactionService.rejectTransaction(transactionId, username, role, body.getReason(), ip);
        return ResponseEntity.ok(java.util.Map.of("rejected", true, "transactionId", transactionId));
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return req.getRemoteAddr();
    }

    /**
     * Resolves the {@link GLApprovalRole} from the {@code gl_approval_role} JWT claim.
     * Throws {@link IllegalStateException} if the user has no GL role assigned, since
     * all endpoints in this controller require the {@code gl:approve} permission which
     * is only granted to GL roles.
     */
    private static GLApprovalRole resolveGlRole(Authentication auth) {
        String roleName = extractClaim(auth, CLAIM_GL_APPROVAL_ROLE);
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalStateException(
                    "Authenticated user has gl:approve permission but no gl_approval_role claim. "
                            + "Ensure the GL approval role is set on the user account.");
        }
        try {
            return GLApprovalRole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown GL approval role in token: " + roleName, e);
        }
    }

    private static String extractClaim(Authentication auth, String claim) {
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString(claim);
        }
        return null;
    }
}
