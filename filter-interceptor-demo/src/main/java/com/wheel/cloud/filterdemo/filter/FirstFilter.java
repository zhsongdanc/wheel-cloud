package com.wheel.cloud.filterdemo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class FirstFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("FirstFilter before, dispatcherType={}, uri={}", request.getDispatcherType(), request.getRequestURI());
        filterChain.doFilter(request, response);
        log.info("FirstFilter after, dispatcherType={}, uri={}", request.getDispatcherType(), request.getRequestURI());
    }
}
