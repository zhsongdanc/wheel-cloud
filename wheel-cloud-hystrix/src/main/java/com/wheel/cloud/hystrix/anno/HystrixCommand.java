package com.wheel.cloud.hystrix.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HystrixCommand {

    String fallbackMethod() default "";

    String groupKey() default "";

    String methodKey() default "";

    String threadPoolKey() default "";

    HystrixProperty[] commandProperties() default {};

    HystrixProperty[] threadPoolProperties() default {};
}
