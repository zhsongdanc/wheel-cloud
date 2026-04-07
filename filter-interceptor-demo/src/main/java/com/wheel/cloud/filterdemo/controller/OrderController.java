package com.wheel.cloud.filterdemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/demo")
public class OrderController {

    @GetMapping("/ok")
    public String ok() {
        log.info("OrderController /demo/ok");
        return "ok";
    }

    @GetMapping("/error")
    public String error() {
        log.info("OrderController /demo/error");
        throw new IllegalStateException("controller error");
    }

    @GetMapping("/echo")
    public Map<String, Object> echo(@RequestParam(required = false) Boolean blockInFilter,
                                    @RequestParam(required = false) Boolean blockInInterceptor) {
        log.info("OrderController /demo/echo");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "echo");
        result.put("blockInFilter", blockInFilter);
        result.put("blockInInterceptor", blockInInterceptor);
        return result;
    }
}
