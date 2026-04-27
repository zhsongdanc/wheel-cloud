package com.wheel.cloud.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * MCP 客户端启动类
 * 启动后自动执行演示：initialize -> listTools -> callTool
 */
@Slf4j
@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "client");
        SpringApplication.run(McpClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo() {
        return args -> {
            McpClient client = new McpClient("http://localhost:8080");

            log.info("=== Step 1: 建立 SSE 连接 ===");
            client.connect();

            log.info("=== Step 2: initialize 握手 ===");
            JsonNode initResult = client.initialize();
            log.info("服务端信息: {}", initResult.path("serverInfo"));

            log.info("=== Step 3: 获取工具列表 ===");
            JsonNode toolsResult = client.listTools();
            log.info("可用工具:");
            toolsResult.path("tools").forEach(tool ->
                    log.info("  - {} : {}", tool.path("name").asText(), tool.path("description").asText())
            );

            log.info("=== Step 4: 调用 get_weather 工具 ===");
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode arguments = mapper.createObjectNode();
            arguments.put("city", "北京");
            JsonNode callResult = client.callTool("get_weather", arguments);
            log.info("工具返回结果:");
            callResult.path("content").forEach(content ->
                    log.info("{}", content.path("text").asText())
            );

            log.info("=== MCP 演示完成 ===");
        };
    }
}
