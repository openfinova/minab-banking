package com.openfinova.banking.tp.api.dto;

/**
 * Configuration class for batch processing operations.
 * Defines parameters for optimizing batch processing performance.
 */
public class BatchProcessingConfig {

    public static final int DEFAULT_BATCH_SIZE = 50;
    public static final int MAX_BATCH_SIZE = 100;
    public static final int DEFAULT_THREAD_POOL_SIZE = 5;
    public static final int MAX_THREAD_POOL_SIZE = 20;
    public static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds

    private int batchSize = DEFAULT_BATCH_SIZE;
    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    private long timeoutMs = DEFAULT_TIMEOUT_MS;
    private boolean failFast = true;
    private boolean enableRetry = true;
    private int maxRetryAttempts = 3;
    private long retryDelayMs = 1000;

    // Constructors
    public BatchProcessingConfig() {
    }

    public BatchProcessingConfig(int batchSize, int threadPoolSize) {
        this.batchSize = Math.min(batchSize, MAX_BATCH_SIZE);
        this.threadPoolSize = Math.min(threadPoolSize, MAX_THREAD_POOL_SIZE);
    }

    // Validation methods
    public void validate() {
        if (batchSize <= 0 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size must be between 1 and " + MAX_BATCH_SIZE);
        }
        if (threadPoolSize <= 0 || threadPoolSize > MAX_THREAD_POOL_SIZE) {
            throw new IllegalArgumentException("Thread pool size must be between 1 and " + MAX_THREAD_POOL_SIZE);
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (maxRetryAttempts < 0) {
            throw new IllegalArgumentException("Max retry attempts cannot be negative");
        }
        if (retryDelayMs < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
    }

    // Getters and Setters
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.min(batchSize, MAX_BATCH_SIZE);
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = Math.min(threadPoolSize, MAX_THREAD_POOL_SIZE);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public boolean isEnableRetry() {
        return enableRetry;
    }

    public void setEnableRetry(boolean enableRetry) {
        this.enableRetry = enableRetry;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public String toString() {
        return "BatchProcessingConfig{" + "batchSize=" + batchSize + ", threadPoolSize=" + threadPoolSize
                + ", timeoutMs=" + timeoutMs + ", failFast=" + failFast + ", enableRetry=" + enableRetry
                + ", maxRetryAttempts=" + maxRetryAttempts + ", retryDelayMs=" + retryDelayMs + '}';
    }
}