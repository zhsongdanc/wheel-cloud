package com.wheel.cloud.hystrix.exception;

public class SemaphoreExceedLimitException extends RuntimeException {
    public SemaphoreExceedLimitException(String message) {
        super(message);
    }
    public SemaphoreExceedLimitException(String message, Throwable cause) {
        super(message, cause);
    }
    public SemaphoreExceedLimitException(Throwable cause) {
        super(cause);
    }
}
