package com.wheel.cloud.eureka.registry.web;

public class RegistryErrorResponse {

    private final String message;

    public RegistryErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
