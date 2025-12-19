package com.wheel.cloud.hystrix.exception;

public class ForbiddenRequestException extends RuntimeException{
    public ForbiddenRequestException(String message) {
        super(message);
    }

    public ForbiddenRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
