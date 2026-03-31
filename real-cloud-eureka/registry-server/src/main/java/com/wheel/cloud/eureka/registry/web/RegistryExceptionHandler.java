package com.wheel.cloud.eureka.registry.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RegistryExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RegistryErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new RegistryErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(RegistryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RegistryErrorResponse handleNotFound(RegistryNotFoundException exception) {
        return new RegistryErrorResponse(exception.getMessage());
    }
}
