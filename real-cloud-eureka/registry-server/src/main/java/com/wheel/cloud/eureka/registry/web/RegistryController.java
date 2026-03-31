package com.wheel.cloud.eureka.registry.web;

import com.wheel.cloud.eureka.registry.model.InstanceInfo;
import com.wheel.cloud.eureka.registry.model.Lease;
import com.wheel.cloud.eureka.registry.model.RegisterInstanceRequest;
import com.wheel.cloud.eureka.registry.model.RegisterResponse;
import com.wheel.cloud.eureka.registry.model.RegistryInstanceView;
import com.wheel.cloud.eureka.registry.model.RegistrySnapshot;
import com.wheel.cloud.eureka.registry.store.InMemoryRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/registry")
public class RegistryController {

    private final InMemoryRegistry registry;

    public RegistryController(InMemoryRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/apps")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody RegisterInstanceRequest request) {
        // 注册时同时创建 lease，后续实例是否有效由续约时间驱动。
        Lease<InstanceInfo> lease = registry.register(request);
        InstanceInfo instanceInfo = lease.getHolder();
        return new RegisterResponse(
                instanceInfo.getServiceName(),
                instanceInfo.getInstanceId(),
                lease.getDurationMs(),
                "instance registered"
        );
    }

    @PutMapping("/apps/{serviceName}/{instanceId}")
    public Map<String, Object> renew(@PathVariable String serviceName, @PathVariable String instanceId) {
        boolean renewed = registry.renew(serviceName, instanceId);
        if (!renewed) {
            throw new RegistryNotFoundException(serviceName, instanceId);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName.toUpperCase());
        response.put("instanceId", instanceId);
        response.put("message", "lease renewed");
        return response;
    }

    @DeleteMapping("/apps/{serviceName}/{instanceId}")
    public Map<String, Object> unregister(@PathVariable String serviceName, @PathVariable String instanceId) {
        boolean removed = registry.unregister(serviceName, instanceId);
        if (!removed) {
            throw new RegistryNotFoundException(serviceName, instanceId);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", serviceName.toUpperCase());
        response.put("instanceId", instanceId);
        response.put("message", "instance unregistered");
        return response;
    }

    @GetMapping("/apps/{serviceName}")
    public List<RegistryInstanceView> getInstances(@PathVariable String serviceName) {
        return registry.getInstances(serviceName);
    }

    @GetMapping("/apps")
    public RegistrySnapshot snapshot() {
        return registry.snapshot();
    }
}
