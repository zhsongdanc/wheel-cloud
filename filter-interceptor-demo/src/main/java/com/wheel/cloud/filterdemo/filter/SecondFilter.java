package com.wheel.cloud.filterdemo.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class SecondFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        log.info("SecondFilter before, dispatcherType={}, uri={}", httpServletRequest.getDispatcherType(), httpServletRequest.getRequestURI());

        if (Boolean.parseBoolean(httpServletRequest.getParameter("blockInFilter"))) {
            log.info("SecondFilter short-circuit, dispatcherType={}, uri={}", httpServletRequest.getDispatcherType(), httpServletRequest.getRequestURI());
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpServletResponse.setContentType("text/plain;charset=UTF-8");
            httpServletResponse.getWriter().write("blocked by SecondFilter");
            return;
        }

        chain.doFilter(request, response);
        log.info("SecondFilter after, dispatcherType={}, uri={}", httpServletRequest.getDispatcherType(), httpServletRequest.getRequestURI());
    }
}
