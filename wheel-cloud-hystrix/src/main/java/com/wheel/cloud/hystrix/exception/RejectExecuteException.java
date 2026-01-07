package com.wheel.cloud.hystrix.exception;

public class RejectExecuteException extends RuntimeException{
    public RejectExecuteException(String message) {
        super(message);
    }
    public RejectExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
