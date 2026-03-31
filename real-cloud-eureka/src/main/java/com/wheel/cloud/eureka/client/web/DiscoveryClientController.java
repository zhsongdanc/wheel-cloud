package com.wheel.cloud.eureka.client.web;

import com.wheel.cloud.eureka.client.EurekaLikeClient;
import com.wheel.cloud.eureka.client.model.ClientCacheView;
import com.wheel.cloud.eureka.client.model.ClientRegistryInstanceView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/client")
public class DiscoveryClientController {

    private final EurekaLikeClient eurekaLikeClient;

    public DiscoveryClientController(EurekaLikeClient eurekaLikeClient) {
        this.eurekaLikeClient = eurekaLikeClient;
    }

    @GetMapping("/cache")
    public ClientCacheView cache() {
        return eurekaLikeClient.getCacheView();
    }

    @GetMapping("/apps/{serviceName}")
    public List<ClientRegistryInstanceView> instances(@PathVariable String serviceName) {
        return eurekaLikeClient.getInstances(serviceName);
    }
}
