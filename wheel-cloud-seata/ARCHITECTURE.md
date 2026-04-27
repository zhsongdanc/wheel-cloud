# wheel-cloud-seata Architecture Notes

## 模块目标

这个模块用于模拟 Seata 的核心实现思路，重点不是“把 Seata 所有特性都抄一遍”，而是按真实角色和状态流转一步步搭起来。

第一阶段默认聚焦：

- AT 模式
- TM / TC / RM 角色划分
- 全局事务与分支事务状态机
- 一个最小可运行的事务协调骨架

## 为什么先做 AT

AT 是最适合学习 Seata 设计味道的切入点，因为它天然会把下面这些问题串起来：

- 为什么需要一个全局事务 ID
- 为什么要有 TC 统一协调
- 为什么 RM 需要向 TC 注册分支事务
- 为什么全局提交和全局回滚不是本地事务能独立完成的

## 第一阶段范围

先做这些：

- `GlobalSession`
- `BranchSession`
- `GlobalTransactionStatus`
- `BranchStatus`
- `TransactionCoordinator`
- `TransactionManager`
- `ResourceManager`

先不做这些：

- JDBC 代理
- undo log
- SQL 解析
- lock table
- 网络通信协议
- 多节点 TC 集群

## 演进路线

1. 先把 TM / TC / RM 的职责边界搭出来
2. 再把全局事务、分支事务的状态流转走通
3. 再考虑 branch register / branch report / global commit / global rollback
4. 最后再决定是否继续模拟 AT 模式里的数据层细节
