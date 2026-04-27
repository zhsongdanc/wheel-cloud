package com.wheel.cloud.apollo.config.web;

import com.wheel.cloud.apollo.config.service.ConfigReadService;
import com.wheel.cloud.apollo.core.model.ConfigRelease;
import com.wheel.cloud.apollo.core.model.NamespaceNotification;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/apollo/config")
public class ConfigController {

    private final ConfigReadService configReadService;

    public ConfigController(ConfigReadService configReadService) {
        this.configReadService = configReadService;
    }

    @GetMapping("/releases/latest")
    public ConfigRelease getLatestRelease(@RequestParam String appId,
                                          @RequestParam String cluster,
                                          @RequestParam String namespace) {
        return configReadService.getLatestRelease(appId, cluster, namespace);
    }

    @GetMapping("/notifications")
    public List<NamespaceNotification> notifications(@RequestParam String appId,
                                                     @RequestParam String cluster,
                                                     @RequestParam String namespaces,
                                                     @RequestParam(defaultValue = "0") long messageId) {
        List<String> namespaceList = StringUtils.hasText(namespaces)
                ? Arrays.asList(namespaces.split(","))
                : Collections.emptyList();
        return configReadService.findUpdatedNamespaces(appId, cluster, namespaceList, messageId);
    }
}
