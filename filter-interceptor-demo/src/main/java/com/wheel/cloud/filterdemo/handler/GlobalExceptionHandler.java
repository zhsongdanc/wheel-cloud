package com.wheel.cloud.filterdemo.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IllegalStateException.class)
    public Map<String, Object> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.info("GlobalExceptionHandler handle IllegalStateException, dispatcherType={}, uri={}", request.getDispatcherType(), request.getRequestURI());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", request.getRequestURI());
        result.put("errorType", ex.getClass().getSimpleName());
        result.put("message", ex.getMessage());
        return result;
    }
}
