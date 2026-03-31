package com.wheel.cloud.eureka.consumer.web;

import com.wheel.cloud.eureka.consumer.client.EurekaLikeClient;
import com.wheel.cloud.eureka.consumer.client.model.ClientCacheView;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistryInstanceView;
import com.wheel.cloud.eureka.consumer.service.ConsumerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {

    private final EurekaLikeClient eurekaLikeClient;
    private final ConsumerService consumerService;

    public ConsumerController(EurekaLikeClient eurekaLikeClient, ConsumerService consumerService) {
        this.eurekaLikeClient = eurekaLikeClient;
        this.consumerService = consumerService;
    }

    @GetMapping("/cache")
    public ClientCacheView cache() {
        return eurekaLikeClient.getCacheView();
    }

    @GetMapping("/apps/{serviceName}")
    public List<ClientRegistryInstanceView> instances(@PathVariable String serviceName) {
        return eurekaLikeClient.getInstances(serviceName);
    }

    @GetMapping("/call/{serviceName}")
    public Map<String, Object> call(@PathVariable String serviceName,
                                    @RequestParam(defaultValue = "world") String name) {
        return consumerService.callProvider(serviceName, name);
    }
}
