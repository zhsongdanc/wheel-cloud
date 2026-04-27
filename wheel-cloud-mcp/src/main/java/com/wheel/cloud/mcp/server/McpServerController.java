package com.wheel.cloud.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP Streamable HTTP 服务端
 *
 * 协议流程：
 * 1. GET  /mcp/sse      — 客户端建立 SSE 长连接，服务端推送 endpoint 事件告知 POST 地址
 * 2. POST /mcp/message  — 客户端发送 JSON-RPC 请求，服务端通过对应 SSE 连接流式返回响应
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpServerController {

    @Autowired
    private McpToolRegistry toolRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    // sessionId -> SseEmitter，维护所有活跃的 SSE 连接
    private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Step 1: 客户端 GET 此端点建立 SSE 连接
     * 服务端立即推送 endpoint 事件，告知客户端应该 POST 到哪个地址
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse() {
        String sessionId = UUID.randomUUID().toString();
        // 超时设置为 0 表示永不超时（由客户端断开连接）
        SseEmitter emitter = new SseEmitter(0L);

        sessions.put(sessionId, emitter);
        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));
        emitter.onError(e -> sessions.remove(sessionId));

        // 推送 endpoint 事件，告知客户端 POST 地址（含 sessionId）
        executor.submit(() -> {
            try {
                String endpointUrl = "/mcp/message?sessionId=" + sessionId;
                emitter.send(SseEmitter.event()
                        .name("endpoint")
                        .data(endpointUrl));
                log.info("[MCP Server] 新连接建立，sessionId={}", sessionId);
            } catch (IOException e) {
                log.error("[MCP Server] 推送 endpoint 事件失败", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Step 2: 客户端 POST JSON-RPC 消息
     * 处理完成后通过 SSE 连接推送响应
     */
    @PostMapping("/message")
    public void message(@RequestParam String sessionId,
                        @RequestBody McpMessage request) {
        SseEmitter emitter = sessions.get(sessionId);
        if (emitter == null) {
            log.warn("[MCP Server] 找不到 sessionId={} 的连接", sessionId);
            return;
        }

        executor.submit(() -> {
            try {
                McpMessage response = handleRequest(request);
                String json = objectMapper.writeValueAsString(response);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(json));
                log.info("[MCP Server] 响应已发送，method={}, id={}", request.getMethod(), request.getId());
            } catch (Exception e) {
                log.error("[MCP Server] 处理请求失败", e);
                try {
                    McpMessage errResp = errorResponse(request.getId(), -32603, "内部错误: " + e.getMessage());
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(objectMapper.writeValueAsString(errResp)));
                } catch (IOException ignored) {}
            }
        });
    }

    private McpMessage handleRequest(McpMessage req) {
        String method = req.getMethod();
        log.info("[MCP Server] 处理请求，method={}", method);

        switch (method) {
            case "initialize":
                return buildInitializeResponse(req.getId());
            case "tools/list":
                return buildToolsListResponse(req.getId());
            case "tools/call":
                return buildToolsCallResponse(req.getId(), req.getParams());
            default:
                return errorResponse(req.getId(), -32601, "不支持的方法: " + method);
        }
    }

    private McpMessage buildInitializeResponse(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");

        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", "wheel-cloud-mcp-server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        Map<String, Object> tools = new HashMap<>();
        tools.put("listChanged", false);
        capabilities.put("tools", tools);
        result.put("capabilities", capabilities);

        McpMessage resp = new McpMessage();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    private McpMessage buildToolsListResponse(String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolRegistry.listTools());

        McpMessage resp = new McpMessage();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    private McpMessage buildToolsCallResponse(String id, JsonNode params) {
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");

        String text = toolRegistry.callTool(toolName, arguments);

        // MCP 规范：content 是 TextContent 数组
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", text);

        Map<String, Object> result = new HashMap<>();
        result.put("content", Collections.singletonList(textContent));
        result.put("isError", false);

        McpMessage resp = new McpMessage();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    private McpMessage errorResponse(String id, int code, String message) {
        McpMessage resp = new McpMessage();
        resp.setId(id);
        resp.setError(new McpMessage.McpError(code, message));
        return resp;
    }
}
