package com.wheel.cloud.filterdemo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@Component
public class ErrorDispatchLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        log.info("ErrorDispatchLoggingFilter before, dispatcherType={}, uri={}", httpServletRequest.getDispatcherType(), httpServletRequest.getRequestURI());
        chain.doFilter(request, response);
        log.info("ErrorDispatchLoggingFilter after, dispatcherType={}, uri={}", httpServletRequest.getDispatcherType(), httpServletRequest.getRequestURI());
    }
}
