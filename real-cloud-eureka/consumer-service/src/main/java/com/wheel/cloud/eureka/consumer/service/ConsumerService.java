package com.wheel.cloud.eureka.consumer.service;

import com.wheel.cloud.eureka.consumer.client.EurekaLikeClient;
import com.wheel.cloud.eureka.consumer.client.model.ClientRegistryInstanceView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        List<ClientRegistryInstanceView> candidates = eurekaLikeClient.getAvailableInstances(serviceName);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("no available instance found in local cache for serviceName=" + serviceName);
        }

        List<String> attemptedInstanceIds = new ArrayList<>();
        RestClientException lastException = null;
        for (ClientRegistryInstanceView target : candidates) {
            String url = "http://" + target.getInstanceInfo().getHost() + ":" + target.getInstanceInfo().getPort()
                    + "/provider/hello?name=" + name;
            attemptedInstanceIds.add(target.getInstanceInfo().getInstanceId());
            try {
                // 先查本地缓存，再根据缓存地址发起调用；失败时切换到缓存里的下一个实例继续尝试。
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
                response.put("attemptedInstanceIds", attemptedInstanceIds);
                response.put("providerResponse", providerResponse);
                return response;
            } catch (RestClientException exception) {
                lastException = exception;
                log.warn("consumer call failed, trying next cached instance if available: serviceName={}, targetInstanceId={}",
                        serviceName, target.getInstanceInfo().getInstanceId(), exception);
            }
        }

        throw new IllegalStateException(
                "all cached provider instances failed for serviceName=" + serviceName + ", attemptedInstanceIds=" + attemptedInstanceIds,
                lastException
        );
    }
}
