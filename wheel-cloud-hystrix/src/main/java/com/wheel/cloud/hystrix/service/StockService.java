package com.wheel.cloud.hystrix.service;

import com.wheel.cloud.hystrix.anno.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class StockService {

    @Resource
    private OrderService orderService;

    @HystrixCommand(fallbackMethod = "defaultPurchase")
    public String purchase(Long stockId, Long num, Long userId) {
        throw new RuntimeException("diyException");
    }


    public String defaultPurchase(Long stockId, Long num, Long userId) {
        return "default";
    }

}
