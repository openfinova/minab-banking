package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.audit.SecurityAuditExtensions;
import com.openfinova.banking.identity.dto.CreateApprovalWorkflowRequest;
import com.openfinova.banking.identity.entity.ApprovalWorkflowInstance;
import com.openfinova.banking.identity.entity.ApprovalWorkflowStatus;
import com.openfinova.banking.identity.entity.ApprovalWorkflowStep;
import com.openfinova.banking.identity.entity.ApprovalWorkflowStepStatus;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.ApprovalWorkflowInstanceRepository;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.validation.GlApprovalRoleValidation;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Service that manages multi-step approval workflows within the identity module.
 *
 * Approval workflows implement a maker-checker control pattern where sensitive operations
 * such as role assignments require one or more designated approvers to sign off before the
 * operation is allowed to proceed. Each workflow is scoped to a resource type and resource
 * ID combination, for example USER_ROLE_ASSIGNMENT and a user UUID, and progresses through
 * an ordered sequence of steps each requiring a specific GL approval role.
 *
 * Only one open workflow may exist for a given resource type and resource ID at any time.
 * Approvals and rejections advance or terminate the workflow accordingly, and every
 * transition is recorded through SecurityAuditService. The resulting APPROVED status is
 * what UserManagementService checks before permitting a role assignment when workflow
 * enforcement is enabled.
 */
@Service
public class IdentityApprovalWorkflowService {

    private static final String OPAQUE_NOT_FOUND = "The requested resource was not found.";

    private final ApprovalWorkflowInstanceRepository workflowRepository;
    private final UserRepository userRepository;
    private final SecurityAuditService auditService;
    private final DateTimeService dateTimeService;

    public IdentityApprovalWorkflowService(ApprovalWorkflowInstanceRepository workflowRepository,
            UserRepository userRepository, SecurityAuditService auditService, DateTimeService dateTimeService) {
        this.workflowRepository = workflowRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Starts a new multi-step approval workflow for the given resource.
     *
     * Checks that no other PENDING or IN_PROGRESS workflow already exists for the same
     * resource type and resource ID combination; if one does an IllegalStateException is
     * thrown immediately. Each GL role code in the ordered list is validated before the
     * workflow is created. The workflow is stored with IN_PROGRESS status, one ordered
     * ApprovalWorkflowStep per required role, and the initiator identity taken from the
     * actor. An APPROVAL_WORKFLOW_STARTED audit event is recorded.
     *
     * @param request  the creation payload specifying resourceType, resourceId, and the
     *                 ordered list of GL approval role codes required to complete the workflow
     * @param actor    the authenticated actor initiating the workflow, used to set the
     *                 initiatorId and record the audit event
     * @return the newly persisted ApprovalWorkflowInstance with IN_PROGRESS status
     * @throws IllegalStateException    if an open workflow already exists for the resource
     * @throws IllegalArgumentException if any required GL role code is invalid
     */
    @Transactional
    public ApprovalWorkflowInstance start(CreateApprovalWorkflowRequest request, AuditActor actor) {
        workflowRepository.findByResourceTypeAndResourceId(request.getResourceType(), request.getResourceId())
                .filter(
                        w -> w.getStatus() == ApprovalWorkflowStatus.PENDING
                                || w.getStatus() == ApprovalWorkflowStatus.IN_PROGRESS)
                .ifPresent(w -> {
                    throw new IllegalStateException(
                            "An open workflow already exists for " + request.getResourceType() + ":"
                                    + request.getResourceId());
                });

        List<String> roles = request.getRequiredGlRolesInOrder();
        roles.forEach(GlApprovalRoleValidation::requireValidOrNull);

        ApprovalWorkflowInstance w = new ApprovalWorkflowInstance(request.getResourceType(), request.getResourceId());
        w.setStatus(ApprovalWorkflowStatus.IN_PROGRESS);
        w.setInitiatorId(actor.userId());
        w.setInitiatorUsername(actor.username());
        IntStream.range(0, roles.size()).forEach(i -> w.addStep(new ApprovalWorkflowStep(i, roles.get(i))));
        ApprovalWorkflowInstance saved = workflowRepository.save(w);
        auditService.recordParticipating(
                SecurityAuditEventType.APPROVAL_WORKFLOW_STARTED,
                actor.userId(),
                actor.username(),
                "Workflow " + saved.getId() + " for " + saved.getResourceType() + ":" + saved.getResourceId(),
                actor,
                SecurityAuditExtensions.NONE,
                AuditEventDetail.workflowStarted(saved.getId(), saved.getResourceType(), saved.getResourceId()));
        return saved;
    }

    /**
     * Records an approval decision for the next pending step in a workflow.
     *
     * The workflow must be open (PENDING or IN_PROGRESS). A maker-checker check prevents the
     * user who initiated the workflow from also approving it. The acting user must have a
     * non-blank glApprovalRole that satisfies the minimum role requirement for the next pending
     * step, as determined by GlApprovalRoleValidation.satisfiesMinimumRole. The step is marked
     * APPROVED with the acting user ID, timestamp from DateTimeService, and optional comment.
     * When no more steps remain pending the workflow transitions to APPROVED status. An
     * APPROVAL_WORKFLOW_STEP_APPROVED audit event is recorded.
     *
     * @param workflowId    the UUID of the workflow to advance
     * @param actingUserId  the UUID of the user performing the approval; must hold a GL role
     *                      sufficient to satisfy the current step
     * @param comment       optional free-text comment stored on the step; may be null
     * @param actor         the authenticated actor for audit recording
     * @return the updated ApprovalWorkflowInstance, either still IN_PROGRESS or now APPROVED
     * @throws ResourceNotFoundException if the workflow or acting user does not exist
     * @throws IllegalStateException     if the workflow is not open or has no pending step
     * @throws SecurityException         if the acting user is the workflow initiator, has no
     *                                   GL role, or the GL role does not satisfy the step
     */
    @Transactional
    public ApprovalWorkflowInstance approve(UUID workflowId, UUID actingUserId, String comment, AuditActor actor) {
        ApprovalWorkflowInstance w = loadWithSteps(workflowId);
        assertOpen(w);

        BankingUser actorUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actingUserId, OPAQUE_NOT_FOUND));

        // Maker-checker: the user who initiated the workflow must not be the one who approves it.
        if (w.getInitiatorId() != null && w.getInitiatorId().equals(actingUserId)) {
            throw new SecurityException(
                    "Maker-checker violation: the workflow initiator cannot approve their own request");
        }

        String glRole = actorUser.getGlApprovalRole();
        if (glRole == null || glRole.isBlank()) {
            throw new SecurityException("User has no glApprovalRole; cannot satisfy workflow step");
        }

        ApprovalWorkflowStep next = w.getSteps().stream()
                .filter(s -> s.getStepStatus() == ApprovalWorkflowStepStatus.PENDING)
                .min(Comparator.comparingInt(ApprovalWorkflowStep::getStepOrder))
                .orElseThrow(() -> new IllegalStateException("No pending approval step"));

        if (!GlApprovalRoleValidation.satisfiesMinimumRole(glRole, next.getRequiredGlApprovalRoleCode())) {
            throw new SecurityException(
                    "User GL role " + glRole + " does not satisfy required " + next.getRequiredGlApprovalRoleCode());
        }

        LocalDateTime now = dateTimeService.now();
        next.setStepStatus(ApprovalWorkflowStepStatus.APPROVED);
        next.setActedByUserId(actingUserId);
        next.setActedAt(now);
        next.setComments(trimComment(comment));

        boolean morePending = w.getSteps().stream()
                .anyMatch(s -> s.getStepStatus() == ApprovalWorkflowStepStatus.PENDING);
        if (!morePending) {
            w.setStatus(ApprovalWorkflowStatus.APPROVED);
        }

        ApprovalWorkflowInstance saved = workflowRepository.save(w);
        auditService.recordParticipating(
                SecurityAuditEventType.APPROVAL_WORKFLOW_STEP_APPROVED,
                actingUserId,
                actorUser.getUsername(),
                "Workflow " + workflowId + " step " + next.getStepOrder(),
                actor,
                SecurityAuditExtensions.NONE,
                AuditEventDetail.workflowStepActed(workflowId, next.getStepOrder(), "APPROVED"));
        return saved;
    }

    /**
     * Records a rejection decision for the next pending step, terminating the workflow.
     *
     * The workflow must be open (PENDING or IN_PROGRESS). The acting user must hold a GL role
     * sufficient to satisfy the current pending step. The step is marked REJECTED and the
     * workflow transitions immediately to REJECTED status with the supplied reason stored as
     * rejectionReason. An APPROVAL_WORKFLOW_REJECTED audit event is recorded.
     *
     * @param workflowId    the UUID of the workflow to reject
     * @param actingUserId  the UUID of the user performing the rejection; must hold a GL role
     *                      sufficient to satisfy the current step
     * @param reason        a human-readable explanation for the rejection, stored on the step
     *                      and on the workflow; may be null
     * @param actor         the authenticated actor for audit recording
     * @return the updated ApprovalWorkflowInstance with REJECTED status
     * @throws ResourceNotFoundException if the workflow or acting user does not exist
     * @throws IllegalStateException     if the workflow is not open or has no pending step
     * @throws SecurityException         if the acting user has no GL role or the GL role does
     *                                   not satisfy the step requirement
     */
    @Transactional
    public ApprovalWorkflowInstance reject(UUID workflowId, UUID actingUserId, String reason, AuditActor actor) {
        ApprovalWorkflowInstance w = loadWithSteps(workflowId);
        assertOpen(w);

        BankingUser actorUser = userRepository.findById(actingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actingUserId, OPAQUE_NOT_FOUND));
        String glRole = actorUser.getGlApprovalRole();
        if (glRole == null || glRole.isBlank()) {
            throw new SecurityException("User has no glApprovalRole; cannot reject workflow step");
        }

        ApprovalWorkflowStep next = w.getSteps().stream()
                .filter(s -> s.getStepStatus() == ApprovalWorkflowStepStatus.PENDING)
                .min(Comparator.comparingInt(ApprovalWorkflowStep::getStepOrder))
                .orElseThrow(() -> new IllegalStateException("No pending approval step"));

        if (!GlApprovalRoleValidation.satisfiesMinimumRole(glRole, next.getRequiredGlApprovalRoleCode())) {
            throw new SecurityException(
                    "User GL role " + glRole + " does not satisfy required " + next.getRequiredGlApprovalRoleCode());
        }

        LocalDateTime now = dateTimeService.now();
        next.setStepStatus(ApprovalWorkflowStepStatus.REJECTED);
        next.setActedByUserId(actingUserId);
        next.setActedAt(now);
        next.setComments(trimComment(reason));

        w.setStatus(ApprovalWorkflowStatus.REJECTED);
        w.setRejectionReason(trimComment(reason));

        ApprovalWorkflowInstance saved = workflowRepository.save(w);
        auditService.recordParticipating(
                SecurityAuditEventType.APPROVAL_WORKFLOW_REJECTED,
                actingUserId,
                actorUser.getUsername(),
                "Workflow " + workflowId + " rejected at step " + next.getStepOrder(),
                actor,
                SecurityAuditExtensions.NONE,
                AuditEventDetail.workflowStepActed(workflowId, next.getStepOrder(), "REJECTED"));
        return saved;
    }

    /**
     * Cancels an open workflow, preventing any further approvals or rejections.
     *
     * The workflow must be open (PENDING or IN_PROGRESS). The status is set to CANCELLED and
     * an APPROVAL_WORKFLOW_CANCELLED audit event is recorded. Cancellation is typically
     * performed by the initiator when the underlying operation is no longer required.
     *
     * @param workflowId  the UUID of the workflow to cancel
     * @param actor       the authenticated actor performing the cancellation, used for audit
     *                    recording
     * @return the updated ApprovalWorkflowInstance with CANCELLED status
     * @throws ResourceNotFoundException if no workflow exists with the given ID
     * @throws IllegalStateException     if the workflow is not currently open
     */
    @Transactional
    public ApprovalWorkflowInstance cancel(UUID workflowId, AuditActor actor) {
        ApprovalWorkflowInstance w = loadWithSteps(workflowId);
        assertOpen(w);
        w.setStatus(ApprovalWorkflowStatus.CANCELLED);
        ApprovalWorkflowInstance saved = workflowRepository.save(w);
        auditService.recordParticipating(
                SecurityAuditEventType.APPROVAL_WORKFLOW_CANCELLED,
                actor.userId(),
                actor.username(),
                "Workflow " + workflowId + " cancelled",
                actor,
                SecurityAuditExtensions.NONE,
                AuditEventDetail.workflowCancelled(workflowId));
        return saved;
    }

    /**
     * Retrieves a single workflow instance including all of its steps.
     *
     * @param id  the unique identifier of the workflow to retrieve
     * @return the matching ApprovalWorkflowInstance with its steps eagerly loaded
     * @throws ResourceNotFoundException if no workflow exists with the given ID
     */
    @Transactional(readOnly = true)
    public ApprovalWorkflowInstance get(UUID id) {
        return loadWithSteps(id);
    }

    /**
     * Returns all workflow instances scoped to the given resource type, ordered by creation
     * date descending.
     *
     * Useful for administrative views that need to display all pending or historical workflows
     * for a particular resource category such as USER_ROLE_ASSIGNMENT.
     *
     * @param resourceType  the resource type string to filter by, for example
     *                      UserManagementService.RESOURCE_TYPE_USER_ROLE_ASSIGNMENT
     * @return all matching ApprovalWorkflowInstance records with steps loaded, newest first;
     *         empty list if none exist for the given resource type
     */
    @Transactional(readOnly = true)
    public List<ApprovalWorkflowInstance> listByResourceType(String resourceType) {
        return workflowRepository.findByResourceTypeWithStepsOrderByCreatedAtDesc(resourceType);
    }

    private ApprovalWorkflowInstance loadWithSteps(UUID id) {
        return workflowRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
    }

    private static void assertOpen(ApprovalWorkflowInstance w) {
        if (w.getStatus() != ApprovalWorkflowStatus.PENDING && w.getStatus() != ApprovalWorkflowStatus.IN_PROGRESS) {
            throw new IllegalStateException("Workflow is not open: " + w.getStatus());
        }
    }

    private static String trimComment(String c) {
        if (c == null || c.isBlank()) {
            return null;
        }
        return c.strip();
    }
}
