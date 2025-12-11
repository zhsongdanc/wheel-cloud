package com.wheel.cloud.hystrix.controller;

import com.wheel.cloud.hystrix.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Resource
    private StockService stockService;

    @GetMapping("/testException")
    public String testException() {
        log.info("testException");
        return stockService.purchaseWithException(1L, 1L, 1L);
    }

    @GetMapping("/testWithoutException")
    public String testFallback() {
        log.info("testWithoutExcption");
        return stockService.purchaseWithoutException(1L, 1L, 1L);
    }
}
