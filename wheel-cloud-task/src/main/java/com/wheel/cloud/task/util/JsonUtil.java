package com.wheel.cloud.task.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

    private JsonUtil() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.registerModule(new JavaTimeModule());
    }

    public static <T> T parseObject(String content, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(content, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse failed: " + content, e);
        }
    }

    public static <T> T parseObject(String content, Class<T> clazz) {
        try {
            return MAPPER.readValue(content, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse failed: " + content, e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize failed: " + obj, e);
        }
    }
}
