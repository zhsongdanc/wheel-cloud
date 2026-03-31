package com.wheel.cloud.eureka.provider.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/provider")
public class ProviderDemoController {

    private static final Logger log = LoggerFactory.getLogger(ProviderDemoController.class);

    @GetMapping("/hello")
    public Map<String, Object> hello(@RequestParam(defaultValue = "world") String name) {
        // provider 的价值在于证明：注册表里保存的地址最后会进入真实业务调用。
        log.info("provider received request: name={}", name);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "hello " + name);
        response.put("provider", "provider-demo");
        return response;
    }
}
