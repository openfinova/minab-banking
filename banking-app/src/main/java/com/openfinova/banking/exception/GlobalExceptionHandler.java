package com.openfinova.banking.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.exchangerate.api.exception.ExchangeRateValidationException;
import com.openfinova.banking.exchangerate.api.exception.InvalidCurrencyPairException;
import com.openfinova.banking.identity.exception.PasswordPolicyViolationException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;

/**
 * Application-wide exception → HTTP response mapper.
 *
 * <p>
 * Produces RFC 7807 {@link ProblemDetail} responses for all controllers.
 * No stack traces are included in responses; full details are logged
 * server-side only.
 *
 * <p>
 * Extends {@link ResponseEntityExceptionHandler} so that Spring MVC's built-in
 * exception handling (e.g. {@code HttpMessageNotReadableException},
 * {@code HttpRequestMethodNotSupportedException}) also returns
 * {@code ProblemDetail}
 * rather than the default Whitelabel error page.
 *
 * <p>
 * Error type URIs follow the pattern {@code /errors/<slug>} — these are
 * identifiers, not necessarily resolvable URLs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        String clientDetail = ex.isOpaqueToClient() ? ex.getClientFacingDetail() : ex.getMessage();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, clientDetail);
        pd.setTitle("Resource Not Found");
        pd.setType(URI.create("/errors/not-found"));
        if (!ex.isOpaqueToClient() && ex.getResourceType() != null) {
            pd.setProperty("resourceType", ex.getResourceType());
            pd.setProperty("resourceId", String.valueOf(ex.getResourceId()));
        }
        return pd;
    }

    @ExceptionHandler(ExchangeRateValidationException.class)
    ProblemDetail handleExchangeRateValidation(ExchangeRateValidationException ex) {
        log.warn("Exchange rate validation failed: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Exchange Rate Validation Error");
        pd.setType(URI.create("/errors/exchange-rate-validation"));
        if (ex.getField() != null) {
            pd.setProperty("field", ex.getField());
            pd.setProperty("rejectedValue", ex.getValue());
        }
        return pd;
    }

    @ExceptionHandler(InvalidCurrencyPairException.class)
    ProblemDetail handleInvalidCurrencyPair(InvalidCurrencyPairException ex) {
        log.warn("Invalid currency pair: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Currency Pair");
        pd.setType(URI.create("/errors/invalid-currency-pair"));
        if (ex.getSourceCurrency() != null) {
            pd.setProperty("sourceCurrency", ex.getSourceCurrency());
            pd.setProperty("targetCurrency", ex.getTargetCurrency());
        }
        return pd;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Entity Not Found");
        pd.setType(URI.create("/errors/not-found"));
        return pd;
    }

    /**
     * Handles concurrent modification detected by JPA optimistic locking
     * ({@code @Version}).
     * Spring wraps {@code jakarta.persistence.OptimisticLockException} in
     * {@link ObjectOptimisticLockingFailureException}; both are caught here so the
     * handler fires regardless of which layer surfaces the exception first.
     *
     * <p>
     * Returns HTTP 409 Conflict — the record was modified by another request
     * between the caller's read and write. The client should re-fetch and retry.
     */
    @ExceptionHandler({ OptimisticLockException.class, ObjectOptimisticLockingFailureException.class })
    ProblemDetail handleOptimisticLock(Exception ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The record was modified by another request. Please re-fetch the current state and retry.");
        pd.setTitle("Concurrent Modification");
        pd.setType(URI.create("/errors/concurrent-modification"));
        return pd;
    }

    /**
     * {@code IllegalArgumentException} is used throughout the codebase as a
     * "not found / bad input" signal (e.g. "GL Account not found: &lt;id&gt;").
     * Map to 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Request");
        pd.setType(URI.create("/errors/invalid-request"));
        return pd;
    }

    @ExceptionHandler(PasswordPolicyViolationException.class)
    ProblemDetail handlePasswordPolicyViolation(PasswordPolicyViolationException ex) {
        log.warn("Password policy violation: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Password Policy Violation");
        pd.setType(URI.create("/errors/password-policy"));
        pd.setProperty("violations", ex.getViolations());
        return pd;
    }

    /**
     * {@code IllegalStateException} is used for business-rule violations
     * (e.g. "Cannot deactivate account with active children").
     * Map to 409 Conflict — the request is valid but conflicts with current state.
     */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Business Rule Violation");
        pd.setType(URI.create("/errors/business-rule-violation"));
        return pd;
    }

    /**
     * {@code SecurityException} is thrown when an operation is refused due to
     * insufficient privileges or a SOD (Separation of Duties) violation
     * (e.g. maker and checker being the same user).
     * Map to 403 Forbidden.
     */
    @ExceptionHandler(SecurityException.class)
    ProblemDetail handleSecurity(SecurityException ex) {
        log.warn("Security violation: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Access Denied");
        pd.setType(URI.create("/errors/access-denied"));
        return pd;
    }

    /**
     * Service-layer authorization (e.g. role-assignment hierarchy) uses Spring
     * Security's
     * {@link AccessDeniedException}; map to 403 like {@link SecurityException}.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleSpringAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Access Denied");
        pd.setType(URI.create("/errors/access-denied"));
        return pd;
    }

    /**
     * Override Spring MVC's default handler for {@code @Valid} failures to
     * include a structured {@code fieldErrors} extension alongside the
     * RFC 7807 body.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        ProblemDetail pd = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields failed validation");
        pd.setTitle("Validation Error");
        pd.setType(URI.create("/errors/validation-error"));
        pd.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("Unreadable HTTP message (JSON/body): {}", ex.getMessage());
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        // Full exception logged server-side; no detail returned to caller
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("/errors/internal"));
        return pd;
    }
}
