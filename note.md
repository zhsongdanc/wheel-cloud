# 学习笔记

本文档用于记录在学习 Spring Cloud 组件实现过程中遇到的困惑点和知识点盲区。

---

## Hystrix（熔断器）

### 状态机
- 待补充

### 滑动窗口
- **2025-12-11 12:00** - 用户对熔断器中是否需要对每次正常请求调用 recordSuccess() 以统计成功率/重置计数的机制不熟悉
  - **知识点说明**：滑动窗口需要记录成功与失败来计算失败率，半开态需基于探测结果收敛，连续失败策略则可能只需失败计数但成功会重置。
  - **相关上下文**：wheel-cloud-hystrix 模块的 CircuitBreaker/HystrixPostProcessor 设计讨论。

### 线程隔离
- 待补充

### 降级策略
- 待补充

### 监控指标
- 待补充

### 设计错误
- **2025-01-XX XX:XX** - CircuitBreaker.allowRequest() 在 OPEN 状态下的逻辑错误
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/CircuitBreaker.java:36-45
  - **错误描述**：即使成功转换为 HALF_OPEN，仍然返回 false，导致请求被拒绝
  - **严重性等级**：Critical
  - **错误分析**：状态转换成功后，应该检查当前状态，如果是 HALF_OPEN 则允许请求
  - **可能后果**：熔断器无法从 OPEN 状态恢复，服务永远无法恢复
  - **修复建议**：转换成功后，重新检查状态，如果是 HALF_OPEN 则允许请求

- **2025-01-XX XX:XX** - HALF_OPEN 状态下的计数逻辑错误
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/CircuitBreaker.java:47
  - **错误描述**：使用 getAndIncrement() 可能导致计数不准确
  - **严重性等级**：Critical
  - **错误分析**：getAndIncrement() 先返回旧值再递增，可能导致判断逻辑错误
  - **可能后果**：探测次数控制不准确，可能允许过多或过少的探测请求
  - **修复建议**：先检查再递增，或使用 incrementAndGet() 并调整判断逻辑

- **2025-01-XX XX:XX** - 拦截器中的异常处理错误
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/HystrixMethodInterceptor.java:35-48
  - **错误描述**：使用 FontFormatException 不合适，catch Exception 会捕获所有异常包括熔断异常
  - **严重性等级**：Critical
  - **错误分析**：熔断器打开时应该直接拒绝请求，而不是走 fallback
  - **可能后果**：熔断器失效，无法真正保护下游服务
  - **修复建议**：使用自定义异常，在 catch 中区分熔断异常和业务异常

- **2025-01-XX XX:XX** - Metrics 使用 SynchronousQueue，不适合滑动窗口
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/analytics/Metrics.java:14-17
  - **错误描述**：SynchronousQueue 只能存储一个元素，无法作为滑动窗口的存储
  - **严重性等级**：Critical
  - **错误分析**：滑动窗口需要存储多个时间点的数据，SynchronousQueue 无法满足需求
  - **可能后果**：无法正确统计失败率，熔断器无法正常工作
  - **修复建议**：改用 ArrayBlockingQueue 或 LinkedBlockingQueue

- **2025-01-XX XX:XX** - InvokeInfo 缺少 startTime 字段
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/HystrixMethodInterceptor.java:77-82
  - **错误描述**：构建 InvokeInfo 时没有设置 startTime 字段
  - **严重性等级**：Critical
  - **错误分析**：clearAllBeforeTimestamp() 需要 startTime 来判断数据是否过期
  - **可能后果**：滑动窗口无法正确清理过期数据，统计不准确
  - **修复建议**：在构建 InvokeInfo 时设置 startTime 字段

- **2025-01-XX XX:XX** - 状态转换后没有更新时间戳
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/CircuitBreaker.java:85-91, 103-112
  - **错误描述**：状态转换为 OPEN 或 HALF_OPEN 后，没有更新 coolDownTimestamp
  - **严重性等级**：High
  - **错误分析**：冷却时间戳用于判断是否可以转换为 HALF_OPEN，必须及时更新
  - **可能后果**：冷却时间判断不准确，可能导致频繁的状态转换
  - **修复建议**：在状态转换时更新时间戳

- **2025-01-XX XX:XX** - CLOSED -> OPEN 的自动转换没有实现
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/CircuitBreaker.java:68-81
  - **错误描述**：checkAndChangeStatus() 中的状态转换逻辑被注释掉了
  - **严重性等级**：High
  - **错误分析**：熔断器的核心功能是根据失败率自动打开，必须实现
  - **可能后果**：熔断器无法自动打开，只能手动控制
  - **修复建议**：实现 CLOSED -> OPEN 的自动转换逻辑

- **2025-01-XX XX:XX** - HALF_OPEN -> CLOSED/OPEN 的转换没有实现
  - **错误位置**：wheel-cloud-hystrix/src/main/java/com/wheel/cloud/hystrix/config/CircuitBreaker.java:103-112
  - **错误描述**：recordSuccess() 和 recordFailed() 中，没有根据 HALF_OPEN 状态进行状态转换
  - **严重性等级**：High
  - **错误分析**：探测请求的结果应该决定状态转换，这是熔断器恢复的关键
  - **可能后果**：熔断器无法从 HALF_OPEN 状态恢复或重新打开
  - **修复建议**：在 recordSuccess/recordFailed 中实现 HALF_OPEN 的状态转换逻辑

---

## Ribbon（负载均衡）

### 负载均衡算法
- 待补充

### 服务列表刷新
- 待补充

### 健康检查
- 待补充

### 设计错误
- 待补充

---

## Feign（声明式 HTTP 客户端）

### 动态代理
- 待补充

### 请求拦截器
- 待补充

### 编解码器
- 待补充

### 设计错误
- 待补充

---

## Eureka（服务注册与发现）

### CAP 权衡
- 待补充

### 心跳机制
- 待补充

### 服务剔除策略
- 待补充

### 设计错误
- 待补充

---

## 通用知识点

### Java 基础
- 待补充

### 设计模式
- 待补充

### 并发编程
- 待补充
