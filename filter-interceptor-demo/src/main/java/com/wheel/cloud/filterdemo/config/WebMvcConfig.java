package com.wheel.cloud.filterdemo.config;

import com.wheel.cloud.filterdemo.interceptor.FirstInterceptor;
import com.wheel.cloud.filterdemo.interceptor.SecondInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private FirstInterceptor firstInterceptor;

    @Autowired
    private SecondInterceptor secondInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(firstInterceptor).addPathPatterns("/**");
        registry.addInterceptor(secondInterceptor).addPathPatterns("/**");
    }
}
