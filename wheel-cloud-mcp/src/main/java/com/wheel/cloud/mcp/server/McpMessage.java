package com.wheel.cloud.mcp.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * JSON-RPC 2.0 消息结构，MCP 协议基于此传输
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {

    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private JsonNode params;
    // 响应字段
    private Object result;
    private McpError error;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class McpError {
        private int code;
        private String message;

        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
