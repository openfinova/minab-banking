package com.openfinova.banking.common.lib.exception;

/**
 * Thrown when a requested resource cannot be found by its identifier.
 *
 * Use this instead of {@link IllegalArgumentException} for "entity not found"
 * cases so that the global exception handler can map them to HTTP 404 rather than 400.
 *
 *   GLAccount account = repository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("GLAccount", id));
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * When non-null, this text is returned to API clients instead of {@link #getMessage()},
     * and {@link #getResourceType()} / {@link #getResourceId()} are omitted from problem details
     * to avoid user or resource enumeration.
     */
    private final String clientFacingDetail;

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        this(resourceType, resourceId, null);
    }

    /**
     * @param clientFacingDetail if non-null, exposed to clients instead of the internal message;
     *                           resource type/id are not exposed in API responses
     */
    public ResourceNotFoundException(String resourceType, Object resourceId, String clientFacingDetail) {
        super(String.format("%s not found: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.clientFacingDetail = clientFacingDetail;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.clientFacingDetail = null;
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Internal message is logged server-side; clients only see {@code clientFacingDetail}.
     */
    public static ResourceNotFoundException opaque(String internalMessage, String clientFacingDetail) {
        return new ResourceNotFoundException(internalMessage, null, null, clientFacingDetail);
    }

    private ResourceNotFoundException(String message, String resourceType, Object resourceId,
            String clientFacingDetail) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.clientFacingDetail = clientFacingDetail;
    }

    public String getClientFacingDetail() {
        return clientFacingDetail;
    }

    public boolean isOpaqueToClient() {
        return clientFacingDetail != null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
