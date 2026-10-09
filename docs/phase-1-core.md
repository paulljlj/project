# 第一阶段：数据库交易核心

## 学习目标

先用 MySQL 完成一条同步下单链路，理解后续引入 Redis、Lua 和 Kafka 时究竟替换了什么、解决了什么问题。

## 当前请求链路

```text
HTTP 请求
  -> Controller：接收并校验参数
  -> Service：执行时间、重复下单和库存规则
  -> Repository：原子扣减库存并保存订单
  -> MySQL：唯一约束兜底“一人一单”
```

## 关键设计

### 原子扣减库存

库存通过一条条件更新语句扣减：

```sql
UPDATE vouchers
SET stock = stock - 1
WHERE id = ? AND stock > 0;
```

受影响行数为 `0` 表示库存不足。判断库存和扣减库存由数据库在同一条语句中完成，避免普通“先查询、再扣减”产生的超卖窗口。

### 一人一单

服务层先查询，提供清楚的错误信息；数据库还有 `(user_id, voucher_id)` 唯一约束作为并发场景下的最终防线。

### 事务边界

扣减库存和创建订单位于同一个事务。如果订单写入失败，库存扣减也会回滚。

## 第一阶段接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/shops` | 创建商家 |
| `GET` | `/api/shops` | 查询商家列表 |
| `GET` | `/api/shops/{id}` | 查询商家详情 |
| `POST` | `/api/vouchers` | 创建优惠券 |
| `GET` | `/api/shops/{shopId}/vouchers` | 查询商家的优惠券 |
| `POST` | `/api/orders` | 下单 |
| `GET` | `/api/orders/{id}` | 查询订单 |

## 本地运行

需要 JDK 21 和 Docker。

```bash
docker compose up -d mysql
./mvnw test
./mvnw spring-boot:run
```

MySQL 对宿主机开放 `3307` 端口，应用默认监听 `8080` 端口。数据库地址和账号可以通过 `HIGO_DB_URL`、`HIGO_DB_USERNAME`、`HIGO_DB_PASSWORD` 环境变量覆盖。

停止数据库：

```bash
docker compose down
```

## 下一阶段

第二阶段已经在商家查询接口前加入 Redis Cache Aside、空值缓存与随机 TTL。继续学习时请按 [本地手写指南](manual-build-guide.md) 从 `learning-start` 标签逐阶段实现。


