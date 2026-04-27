package com.wheel.cloud.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.wheel.cloud.mcp.server.tool.WeatherTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具注册表，统一管理所有可调用工具
 */
@Component
public class McpToolRegistry {

    @Autowired
    private WeatherTool weatherTool;

    /** 返回 tools/list 响应中的工具列表 */
    public List<Map<String, Object>> listTools() {
        Map<String, Object> weather = new HashMap<>();
        weather.put("name", WeatherTool.NAME);
        weather.put("description", WeatherTool.DESCRIPTION);
        weather.put("inputSchema", WeatherTool.inputSchema());
        return Collections.singletonList(weather);
    }

    /** 根据工具名分发调用 */
    public String callTool(String toolName, JsonNode arguments) {
        if (WeatherTool.NAME.equals(toolName)) {
            return weatherTool.execute(arguments);
        }
        throw new IllegalArgumentException("未知工具: " + toolName);
    }
}
