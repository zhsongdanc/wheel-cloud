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
- **2025-12-15 12:00** - 用户对熔断器指标数据结构的设计选择不熟悉（逐请求排队 vs 桶化聚合）以及 Hystrix/Sentinel 如何存储窗口数据
  - **知识点说明**：经典实现会用时间分片（buckets）累计计数而非存全量请求，避免大队列占内存；每个桶保存成功/失败/超时等计数，滑动窗口通过定时/时间戳驱逐过期桶。
  - **相关上下文**：wheel-cloud-hystrix 模块 Metrics 现用 ArrayBlockingQueue 逐请求入队，用户提问“Metric 类的数据结构设计不太好，不知道 Hystrix/Sentinel 怎么做的”。
- **2025-12-15 12:10** - 用户不确定滑动窗口按“请求开始时间”还是“请求结束时间”分桶
  - **知识点说明**：熔断统计通常以请求开始时间入桶，用于反映触发时刻的负载与失败率；若用结束时间，长尾请求会把旧请求计入新桶，导致时序漂移。也可在慢调用统计中同时记录耗时分位，分桶键仍以开始时间为主。
  - **相关上下文**：用户询问“应该按照请求时间还是响应时间将请求划分到不同的桶里”。
- 待补充

### 设计错误
- **2025-12-15 12:00** - Metrics 逐请求入队且使用有界队列，满时直接抛异常
  - **错误上下文**：熔断器监控指标采集/滑动窗口统计模块；recordSuccess/recordFailed 每个调用都向 ArrayBlockingQueue 加入一条记录，未做丢弃或降采样。
  - **错误描述**：当请求量高于队列容量（当前 10w）时，BlockingQueue.add 会直接抛 IllegalStateException，导致业务调用路径在统计阶段异常；同时逐请求存储会造成内存和清理成本线性增长，无法满足滚动窗口的高吞吐需求。
  - **严重性等级**：High
  - **错误分析**：缺少按时间分片的聚合桶设计，违反滑动窗口应“按时间桶累加计数”的原则；也缺少背压/丢弃策略，违反监控侧不能反噬业务的原则。
  - **可能后果**：高并发下请求会因统计异常而失败；内存占用随请求量激增；窗口清理成本升高导致延迟抖动。
  - **修复建议**：改用固定桶数的时间窗口（如 10 个 1s 桶）累计计数，并在桶交换时重置；对超出窗口的事件直接覆盖或丢弃，避免逐请求入队；如需明细日志，应独立异步管道处理。
  - **相关上下文**：用户质疑 Metrics 数据结构设计，并询问 Hystrix/Sentinel 的做法。
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
- **2025-12-15 12:05** - 用户混淆“阻塞队列用于线程池工作队列”与“熔断指标滑动窗口的数据结构选择”场景
  - **知识点说明**：线程池的阻塞队列承担任务排队与生产者-消费者协作，需要阻塞/背压语义；熔断统计是高频计数，目标是无阻塞、常数空间，宜用时间桶（数组或 ConcurrentHashMap）累加而非排队。
  - **相关上下文**：用户问“为什么线程池用阻塞队列，这个需要用 ConcurrentHashMap”，暴露对两类队列/数据结构职责的混淆。

### 并发编程
- 待补充
