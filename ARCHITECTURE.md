# wheel-cloud Architecture Notes

这个仓库默认被视为一个学习后端架构的多模块实验项目，重点关注：

- Spring Cloud
- Spring Cloud Alibaba
- RPC
- Netty
- 分布式系统中的治理、通信、容错与演进

后续协作默认遵循这几个原则：

- 在直接给方案或写代码前，先提出值得思考的问题
- 先讨论问题本质，再讨论技术选型
- 每次有新的设计结论，都尽量沉淀到对应模块文档中
- 优先记录“为什么这样设计”，而不只是“最后怎么做”

## How We Learn

后续讨论一个设计点时，尽量按下面顺序推进：

1. 这个问题在分布式系统里到底解决什么痛点？
2. 如果不用这个设计，最朴素的实现会遇到什么问题？
3. 这个方案的核心思想是什么？
4. 这个方案的代价、边界和适用场景是什么？
5. 在当前项目里，应该落在哪个模块、哪一层代码？

## Module Notes

- [wheel-cloud-hystrix/ARCHITECTURE.md](/Users/songzehui02/learn/wheel-cloud/wheel-cloud-hystrix/ARCHITECTURE.md)
- [real-cloud-eureka/ARCHITECTURE.md](/Users/songzehui02/learn/wheel-cloud/real-cloud-eureka/ARCHITECTURE.md)
- [rpc-demo/ARCHITECTURE.md](/Users/songzehui02/learn/wheel-cloud/rpc-demo/ARCHITECTURE.md)
- [db-export-oss/ARCHITECTURE.md](/Users/songzehui02/learn/wheel-cloud/db-export-oss/ARCHITECTURE.md)

## Note Template

后面如果你说“帮我记录下来”，我会优先补充到对应模块文档的这些部分：

- 模块目标
- 当前实现
- 核心设计思想
- 值得追问的问题
- 方案权衡
- 演进路线
- 待验证假设

## Cross-Module Thinking

这个项目后续可以重点串联这些学习主线：

- `real-cloud-eureka`：服务注册与发现为什么是微服务基础设施
- `wheel-cloud-hystrix`：隔离、熔断、限流为什么是服务治理能力
- `rpc-demo`：服务之间为什么不能只停留在 HTTP 调用视角
- `db-export-oss`：一个相对偏业务/工具的模块，适合讨论边界、复用和工程化

## Current Collaboration Agreement

我后续会默认这样配合你：

- 写代码前先提出一些关键问题引导你思考
- 核心架构点会优先讲设计思想和权衡
- 需要沉淀时，直接帮你更新对应模块文档
- 既讲“怎么做”，也讲“为什么这样做”
