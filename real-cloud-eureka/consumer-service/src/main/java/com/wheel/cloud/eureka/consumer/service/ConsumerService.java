package com.wheel.cloud.eureka.consumer.service;

import com.wheel.cloud.eureka.consumer.client.EurekaLikeClient;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistryInstanceView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConsumerService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerService.class);

    private final EurekaLikeClient eurekaLikeClient;
    private final RestTemplate restTemplate;

    public ConsumerService(EurekaLikeClient eurekaLikeClient, RestTemplateBuilder restTemplateBuilder) {
        this.eurekaLikeClient = eurekaLikeClient;
        this.restTemplate = restTemplateBuilder.build();
    }

    public Map<String, Object> callProvider(String serviceName, String name) {
        ClientRegistryInstanceView target = eurekaLikeClient.chooseFirstInstance(serviceName);
        if (target == null) {
            throw new IllegalStateException("no available instance found in local cache for serviceName=" + serviceName);
        }

        // 先查本地缓存，再根据缓存地址发起调用，这才是 Eureka 最关键的消费侧行为。
        String url = "http://" + target.getInstanceInfo().getHost() + ":" + target.getInstanceInfo().getPort()
                + "/provider/hello?name=" + name;
        log.info("consumer calling provider: serviceName={}, targetInstanceId={}, url={}",
                serviceName, target.getInstanceInfo().getInstanceId(), url);
        Map<?, ?> providerResponse = restTemplate.getForObject(url, Map.class);
        log.info("consumer received provider response: serviceName={}, targetInstanceId={}",
                serviceName, target.getInstanceInfo().getInstanceId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName);
        response.put("targetInstanceId", target.getInstanceInfo().getInstanceId());
        response.put("targetHost", target.getInstanceInfo().getHost());
        response.put("targetPort", target.getInstanceInfo().getPort());
        response.put("providerResponse", providerResponse);
        return response;
    }
}
