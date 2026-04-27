package com.wheel.cloud.apollo.client.web;

import com.wheel.cloud.apollo.client.MockApolloClient;
import com.wheel.cloud.apollo.client.model.ClientConfigCacheView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apollo/client")
public class ClientController {

    private final MockApolloClient mockApolloClient;

    public ClientController(MockApolloClient mockApolloClient) {
        this.mockApolloClient = mockApolloClient;
    }

    @GetMapping("/refresh")
    public ClientConfigCacheView refresh(@RequestParam String appId,
                                         @RequestParam String cluster,
                                         @RequestParam String namespace) {
        return mockApolloClient.refresh(appId, cluster, namespace);
    }

    @GetMapping("/cache")
    public ClientConfigCacheView cache() {
        return mockApolloClient.getCacheView();
    }
}
