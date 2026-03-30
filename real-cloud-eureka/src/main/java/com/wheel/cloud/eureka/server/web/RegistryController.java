package com.wheel.cloud.eureka.server.web;

import com.wheel.cloud.eureka.server.model.Lease;
import com.wheel.cloud.eureka.server.model.InstanceInfo;
import com.wheel.cloud.eureka.server.model.RegisterInstanceRequest;
import com.wheel.cloud.eureka.server.model.RegisterResponse;
import com.wheel.cloud.eureka.server.model.RegistryInstanceView;
import com.wheel.cloud.eureka.server.model.RegistrySnapshot;
import com.wheel.cloud.eureka.server.registry.InMemoryRegistry;
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
        // 注册时同时创建租约，后续实例是否仍然有效将由续约时间驱动。
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
        // 续约只刷新 lease 的时间状态，不改实例的静态信息。
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
        // 主动下线是辅助机制，真正兜底实例失效的是服务端的过期剔除任务。
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
        // 这里先保留完整视图，方便学习实例信息和租约状态是如何组合返回的。
        return registry.getInstances(serviceName);
    }

    @GetMapping("/apps")
    public RegistrySnapshot snapshot() {
        // 快照接口更偏向观察注册表内部状态，而不是面向生产消费的服务发现接口。
        return registry.snapshot();
    }
}
