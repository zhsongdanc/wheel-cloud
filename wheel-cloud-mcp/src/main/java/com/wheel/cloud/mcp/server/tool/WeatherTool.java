package com.wheel.cloud.mcp.server.tool;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例工具：查询天气（mock 数据）
 */
@Component
public class WeatherTool {

    public static final String NAME = "get_weather";
    public static final String DESCRIPTION = "获取指定城市的天气信息";

    /** 工具的 inputSchema，符合 JSON Schema 规范 */
    public static Map<String, Object> inputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> cityProp = new HashMap<>();
        cityProp.put("type", "string");
        cityProp.put("description", "城市名称，如 北京、上海");

        Map<String, Object> properties = new HashMap<>();
        properties.put("city", cityProp);
        schema.put("properties", properties);
        schema.put("required", new String[]{"city"});
        return schema;
    }

    public String execute(JsonNode arguments) {
        String city = arguments.path("city").asText("未知城市");
        // mock 数据，实际可接入真实天气 API
        Map<String, String> mockData = new HashMap<>();
        mockData.put("北京", "晴，25°C，微风");
        mockData.put("上海", "多云，22°C，东南风");
        mockData.put("广州", "阵雨，28°C，南风");

        String weather = mockData.getOrDefault(city, "晴，20°C，无风（mock）");
        return String.format("城市：%s\n天气：%s", city, weather);
    }
}
