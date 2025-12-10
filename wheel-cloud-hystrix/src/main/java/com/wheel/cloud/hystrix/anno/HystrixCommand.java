package com.wheel.cloud.hystrix.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HystrixCommand {

    String fallbackMethod() default "";
}
