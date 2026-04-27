# wheel-cloud-apollo Architecture Notes

## 模块目标

这个模块用于模拟 Apollo 配置中心的核心架构思路，重点不是把 Apollo 全量复刻出来，而是保留它最有代表性的职责拆分和配置流转设计。

第一阶段聚焦：

- Admin Service
- Config Service
- ReleaseMessage
- Client Local Cache
- Namespace / Cluster / AppId 基础模型

## 第一阶段范围

先做这些：

- 配置发布
- 最新配置读取
- 基于消息 ID 的变更通知
- 客户端本地缓存刷新

先不做这些：

- Portal UI
- Meta Server / Eureka
- 长轮询实时推送
- 灰度发布
- 权限审批

## 演进路线

1. 先把发布、读取、通知三条主链路跑通
2. 再补客户端本地缓存和后台刷新
3. 再考虑长轮询和实时推送
