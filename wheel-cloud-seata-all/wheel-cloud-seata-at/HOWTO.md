# wheel-cloud-seata-at 使用说明

## 数据库

已自动建好，无需手动操作。

| 库 | 表 | 初始数据 |
|---|---|---|
| `seata_order` | `t_order`, `undo_log` | 空 |
| `seata_stock` | `t_stock`, `undo_log` | 商品1库存100，商品2库存50 |
| `seata_account` | `t_account`, `undo_log` | 用户1余额1000，用户2余额500 |

## 启动前：先跑 Seata Server（TC）

Seata Server 是 AT 模式的协调者，监听 `8091` 端口。

```bash
# 方式一：Docker（推荐）
docker run -d --name seata-server \
  -p 8091:8091 \
  -e SEATA_CONFIG_NAME=file \
  seataio/seata-server:1.7.0

# 方式二：下载 seata-server-1.7.0.tar.gz 解压后执行
sh bin/seata-server.sh -h 127.0.0.1 -p 8091
```

## 启动应用

应用端口 `8092`（避开 Seata Server 的 8091）：

```bash
mvn spring-boot:run -pl wheel-cloud-seata-at
```

## 测试接口

### 1. 正常下单（全局提交）

```
GET http://localhost:8092/seata/at/commit
```

三个库数据全部变更，`undo_log` 写入后被自动清空。

### 2. 触发全局回滚（余额不足）

```
GET http://localhost:8092/seata/at/rollback
```

库存分支已执行 → TC 收到回滚信号 → 用 `undo_log` 反向 SQL 恢复库存。

### 3. 手动抛异常验证全局回滚

```
GET http://localhost:8092/seata/at/manual-rollback
```

三步全部执行，最后抛异常 → 三个库全部回滚，验证 Seata 的最终一致性。

### 4. 查看当前数据快照

```
GET http://localhost:8092/seata/at/state
```

### 5. 重置数据（方便反复测试）

```
POST http://localhost:8092/seata/at/reset
```

## 关键代码

| 知识点 | 文件 |
|---|---|
| `@GlobalTransactional` 开启全局事务 | `common/PlaceOrderService.java` |
| `DataSourceProxy` 数据源代理（AT 核心） | `common/DataSourceConfig.java` |
| 三库各自的 `@Transactional` 分支事务 | `order/OrderService.java` `stock/StockService.java` `account/AccountService.java` |
| undo_log 表（AT 自动写入/清空） | 三个库各自的 `undo_log` 表 |
