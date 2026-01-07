package com.wheel.cloud.hystrix.service;

import com.wheel.cloud.hystrix.anno.HystrixCommand;
import com.wheel.cloud.hystrix.anno.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class StockService {

    @Resource
    private OrderService orderService;

    @HystrixCommand(fallbackMethod = "defaultPurchase")
    @RateLimit(algorithm = 1, permitsPerSecond = 1)
    public String purchaseWithException(Long stockId, Long num, Long userId) {
        throw new RuntimeException("diyException");
    }

    @HystrixCommand(fallbackMethod = "defaultPurchase")
    public String purchaseWithoutException(Long stockId, Long num, Long userId) {
        return "withoutException";
    }


    private String defaultPurchase(Long stockId, Long num, Long userId) {
        return "default";
    }

}
