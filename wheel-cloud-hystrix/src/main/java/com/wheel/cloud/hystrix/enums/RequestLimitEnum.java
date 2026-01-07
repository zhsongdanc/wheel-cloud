package com.wheel.cloud.hystrix.enums;

import lombok.Getter;

@Getter
public enum RequestLimitEnum {

    FIXED_WINDOW(1),
    SLIDING_WINDOW(2),
    LEAKY_BUCKET(3),
    TOKEN_BUCKET(4);

    private final int code;

    RequestLimitEnum(int code) {
        this.code = code;
    }
}