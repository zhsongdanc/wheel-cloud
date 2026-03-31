package com.wheel.cloud.eureka;

import com.wheel.cloud.eureka.server.RegistryProperties;
import com.wheel.cloud.eureka.client.DiscoveryClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({RegistryProperties.class, DiscoveryClientProperties.class})
public class RealCloudEurekaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealCloudEurekaApplication.class, args);
    }
}
