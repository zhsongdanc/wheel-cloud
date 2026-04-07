package com.wheel.cloud.filterdemo.config;

import com.wheel.cloud.filterdemo.filter.FirstFilter;
import com.wheel.cloud.filterdemo.filter.SecondFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<FirstFilter> firstFilterRegistrationBean(FirstFilter firstFilter) {
        FilterRegistrationBean<FirstFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(firstFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<SecondFilter> secondFilterRegistrationBean(SecondFilter secondFilter) {
        FilterRegistrationBean<SecondFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(secondFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
