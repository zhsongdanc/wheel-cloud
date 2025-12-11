package com.wheel.cloud.hystrix.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassUtil {

    public static String generateMethodKey(String className, String methodName, Class<?>[] clazzs) {
        if (clazzs == null) {
            return className + "." + methodName;
        }
        return className + "#" + methodName + "("+ Arrays.stream(clazzs).map(clazz -> {
            if (clazz.isPrimitive()) {
                return clazz.getName();
            }
            return clazz.getSimpleName();
        }).collect(Collectors.joining(",")) + ")";
    }


    public static void main(String[] args) {
        System.out.println(generateMethodKey("com.wheel.cloud.hystrix.util.ClassUtil", "test", null));
        System.out.println(generateMethodKey("com.wheel.cloud.hystrix.util.ClassUtil", "test", new Class[]{String.class, int.class}));
    }

    public static String test(String a, int b) {
        return a + b;
    }
}
