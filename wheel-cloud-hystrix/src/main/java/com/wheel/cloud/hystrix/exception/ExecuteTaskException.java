package com.wheel.cloud.hystrix.exception;

public class ExecuteTaskException extends RuntimeException {
    public ExecuteTaskException(Throwable cause) {
        super(cause);
    }

    public ExecuteTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
