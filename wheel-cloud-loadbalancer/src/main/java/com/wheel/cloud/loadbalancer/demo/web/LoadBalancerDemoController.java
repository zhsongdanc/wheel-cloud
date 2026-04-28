package com.wheel.cloud.loadbalancer.demo.web;

import com.wheel.cloud.loadbalancer.demo.service.LoadBalancerDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/loadbalancer")
public class LoadBalancerDemoController {

    private final LoadBalancerDemoService loadBalancerDemoService;

    public LoadBalancerDemoController(LoadBalancerDemoService loadBalancerDemoService) {
        this.loadBalancerDemoService = loadBalancerDemoService;
    }

    @GetMapping("/choose")
    public Map<String, Object> choose(@RequestParam String serviceName,
                                      @RequestParam(defaultValue = "round-robin") String rule) {
        return loadBalancerDemoService.choose(serviceName, rule);
    }

    @GetMapping("/instances")
    public Map<String, Object> instances(@RequestParam String serviceName) {
        return loadBalancerDemoService.list(serviceName);
    }

    @GetMapping("/fail")
    public Map<String, Object> fail(@RequestParam String serviceName,
                                    @RequestParam String instanceId) {
        return loadBalancerDemoService.fail(serviceName, instanceId);
    }
}
