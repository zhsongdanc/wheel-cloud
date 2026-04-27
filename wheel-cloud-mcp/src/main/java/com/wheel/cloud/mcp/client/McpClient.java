package com.wheel.cloud.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MCP Streamable HTTP 客户端
 *
 * 使用流程：
 * 1. connect()    — GET /mcp/sse，建立 SSE 连接，获取 postEndpoint
 * 2. initialize() — 握手
 * 3. listTools()  — 获取工具列表
 * 4. callTool()   — 调用工具
 */
@Slf4j
public class McpClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    // SSE 连接建立后，服务端告知的 POST 端点路径
    private final AtomicReference<String> postEndpoint = new AtomicReference<>();

    // requestId -> 等待响应的 CompletableFuture
    private final Map<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    public McpClient(String serverBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(serverBaseUrl)
                .build();
    }

    /**
     * 建立 SSE 长连接，阻塞直到收到 endpoint 事件
     */
    public void connect() throws Exception {
        CountDownLatch endpointLatch = new CountDownLatch(1);

        Flux<ServerSentEvent<String>> sseFlux = webClient.get()
                .uri("/mcp/sse")
                .retrieve()
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {});

        sseFlux.subscribe(
                event -> handleSseEvent(event, endpointLatch),
                error -> log.error("[MCP Client] SSE 连接错误", error),
                () -> log.info("[MCP Client] SSE 连接关闭")
        );

        // 等待 endpoint 事件（最多 5 秒）
        if (!endpointLatch.await(5, TimeUnit.SECONDS)) {
            throw new TimeoutException("等待 MCP server endpoint 超时");
        }
        log.info("[MCP Client] 连接成功，postEndpoint={}", postEndpoint.get());
    }

    private void handleSseEvent(ServerSentEvent<String> event, CountDownLatch latch) {
        String eventName = event.event();
        String data = event.data();
        log.debug("[MCP Client] 收到 SSE 事件，name={}, data={}", eventName, data);

        if ("endpoint".equals(eventName)) {
            postEndpoint.set(data);
            latch.countDown();
        } else if ("message".equals(eventName)) {
            try {
                JsonNode node = objectMapper.readTree(data);
                String id = node.path("id").asText(null);
                if (id != null) {
                    CompletableFuture<JsonNode> future = pendingRequests.remove(id);
                    if (future != null) {
                        future.complete(node);
                    }
                }
            } catch (Exception e) {
                log.error("[MCP Client] 解析 message 事件失败", e);
            }
        }
    }

    /**
     * 发送 JSON-RPC 请求，返回响应 result 节点
     */
    public JsonNode send(String method, ObjectNode params) throws Exception {
        String id = String.valueOf(idCounter.getAndIncrement());

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            request.set("params", params);
        }

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        String endpoint = postEndpoint.get();
        if (endpoint == null) {
            throw new IllegalStateException("尚未建立 SSE 连接，请先调用 connect()");
        }

        webClient.post()
                .uri(endpoint)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(request.toString())
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        resp -> log.debug("[MCP Client] POST 已发送，method={}, id={}", method, id),
                        error -> {
                            pendingRequests.remove(id);
                            future.completeExceptionally(error);
                        }
                );

        // 等待 SSE 响应（最多 10 秒）
        JsonNode response = future.get(10, TimeUnit.SECONDS);

        JsonNode error = response.get("error");
        if (error != null) {
            throw new RuntimeException("MCP 错误: " + error.path("message").asText());
        }
        return response.get("result");
    }

    /** MCP 握手 */
    public JsonNode initialize() throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("protocolVersion", "2024-11-05");

        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "wheel-cloud-mcp-client");
        clientInfo.put("version", "1.0.0");
        params.set("clientInfo", clientInfo);

        params.set("capabilities", objectMapper.createObjectNode());

        return send("initialize", params);
    }

    /** 获取工具列表 */
    public JsonNode listTools() throws Exception {
        return send("tools/list", null);
    }

    /** 调用工具 */
    public JsonNode callTool(String toolName, ObjectNode arguments) throws Exception {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments);
        return send("tools/call", params);
    }
}
