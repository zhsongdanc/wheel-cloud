package com.wheel.cloud.apollo.admin.web;

import com.wheel.cloud.apollo.admin.model.PublishNamespaceRequest;
import com.wheel.cloud.apollo.admin.service.AdminConfigService;
import com.wheel.cloud.apollo.core.model.ConfigRelease;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apollo/admin")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    public AdminConfigController(AdminConfigService adminConfigService) {
        this.adminConfigService = adminConfigService;
    }

    @PostMapping("/publish")
    public ConfigRelease publish(@RequestBody PublishNamespaceRequest request) {
        return adminConfigService.publish(request);
    }
}
