package com.wheel.cloud.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        // 启动时指定 server profile，端口 8080
        System.setProperty("spring.profiles.active", "server");
        SpringApplication.run(McpServerApplication.class, args);
    }
}
