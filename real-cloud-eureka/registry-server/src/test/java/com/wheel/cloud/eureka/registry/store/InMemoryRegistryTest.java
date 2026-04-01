package com.wheel.cloud.eureka.registry.store;

import com.wheel.cloud.eureka.registry.config.RegistryProperties;
import com.wheel.cloud.eureka.registry.model.RegisterInstanceRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRegistryTest {

    @Test
    void shouldRegisterRenewAndEvictInstance() throws InterruptedException {
        RegistryProperties properties = new RegistryProperties();
        properties.setLeaseDurationMs(50L);
        InMemoryRegistry registry = new InMemoryRegistry(properties, new RegistryDeltaLog());

        RegisterInstanceRequest request = new RegisterInstanceRequest();
        request.setServiceName("order-service");
        request.setInstanceId("order-service-1");
        request.setHost("127.0.0.1");
        request.setPort(8081);
        request.setMetadata(Collections.singletonMap("zone", "learn"));

        registry.register(request);
        assertEquals(1, registry.getInstances("order-service").size());

        assertTrue(registry.renew("order-service", "order-service-1"));
        Thread.sleep(80L);

        assertEquals(1, registry.evictExpiredInstances().size());
        assertTrue(registry.getInstances("order-service").isEmpty());
    }

    @Test
    void shouldReturnFalseWhenRenewMissingInstance() {
        RegistryProperties properties = new RegistryProperties();
        InMemoryRegistry registry = new InMemoryRegistry(properties, new RegistryDeltaLog());

        assertFalse(registry.renew("payment-service", "payment-service-1"));
        assertFalse(registry.unregister("payment-service", "payment-service-1"));
    }
}
