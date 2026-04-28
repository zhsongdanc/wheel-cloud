# wheel-cloud-loadbalancer Architecture Notes

## 模块目标

这个模块用于模拟客户端负载均衡的核心设计，重点参考 Ribbon 的抽象方式，而不是直接埋进 Spring Cloud 具体实现细节。

第一阶段聚焦：

- `ServiceInstance`
- `ServiceInstanceSupplier`
- `LoadBalanceRule`
- `LoadBalancer`

## 核心设计思想

- 服务发现负责“拿到实例列表”
- 负载均衡负责“从实例列表里选一个”
- 两者应该解耦，否则组件边界会混乱

## 第一阶段范围

先做这些：

- RoundRobin
- Random
- Weighted Random
- 简单的本地失败实例临时剔除

先不做这些：

- 和 Eureka 模块直接联动
- Reactor / WebClient 集成
- 真实健康检查
- 区域感知、最少连接等更复杂策略
