# DianpingPlus 独立复现说明

## 复现边界

本项目参考 `Dylan4real/dianpingPlus` 的业务能力和学习主题，代码基于 Java 21、Spring Boot 3.5、JPA、Redis 7、Kafka 3 和 MySQL 8 独立实现。参考仓库没有明确的项目级许可证，因此这里不复制它的源码、包名或数据库脚本。

| DianpingPlus 能力 | 嗨Go实现 | 主要学习点 |
| --- | --- | --- |
| 手机验证码登录 | `/api/auth/codes`、`/api/auth/sessions` | Redis TTL、发送频控、滑动会话 |
| 商家缓存 | `ShopCache` | Cache Aside、空值缓存、随机 TTL |
| 附近商家 | `/api/shops/search` | Redis GEO、按距离分页 |
| 秒杀 | `/api/seckill/vouchers/{id}/orders` | Lua 原子预检、库存与一人一单 |
| 异步下单 | `SeckillOrderConsumer` | Kafka 削峰、请求号幂等、数据库兜底 |
| 签到 | `/api/sign-ins` | Redis Bitmap、连续签到统计 |
| 关注与共同关注 | `/api/follows` | 数据库关系、Redis Set 交集 |
| 博客与 Feed | `/api/blogs` | ZSet 点赞、推模式收件箱 |
| 限流 | `@RateLimit` | AOP、Lua、滑动窗口 |

## 请求链路

### 查询商家

```text
GET /api/shops/{id}
  -> 查询 Redis
  -> 命中 JSON：直接返回
  -> 命中空值标记：返回 404
  -> 未命中：查询 MySQL
  -> 写入随机 TTL 缓存或短期空值缓存
```

商家更新遵循“先更新数据库，再删除缓存”。随机增加 0～5 分钟 TTL，避免大量 Key 同时失效。

### 秒杀下单

```text
HTTP 请求
  -> 登录会话 + 用户维度限流
  -> Lua 原子检查 Redis 库存和购买集合
  -> Kafka 发送 SeckillOrderMessage
  -> 立即返回 requestId 和 ACCEPTED
  -> Kafka 消费者开启数据库事务
  -> requestId 幂等检查
  -> MySQL 条件扣减库存
  -> (user_id, voucher_id) 唯一约束兜底
  -> 保存订单
```

Lua 成功而 Kafka 发送失败时，补偿脚本会移除用户购买标记并归还 Redis 库存。数据库写入失败时 Kafka 不确认消息，交给消费重试处理。

## 本地启动

需要 JDK 21 与 Docker：

```bash
docker compose up -d
./mvnw test
./mvnw spring-boot:run
```

基础设施端口：MySQL `3307`、Redis `6379`、Kafka `9092`。

## 手工体验顺序

### 1. 创建带坐标的商家

```bash
curl -X POST http://localhost:8080/api/shops \
  -H 'Content-Type: application/json' \
  -d '{"name":"海珠咖啡","category":"咖啡","address":"广州海珠","longitude":113.317,"latitude":23.083}'
```

### 2. 获取验证码并登录

学习环境默认会在响应中返回验证码：

```bash
curl -X POST http://localhost:8080/api/auth/codes \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800138000"}'

curl -X POST http://localhost:8080/api/auth/sessions \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800138000","code":"响应中的六位验证码"}'
```

后续受保护接口携带 `Authorization: Bearer <token>`。

### 3. 创建优惠券并秒杀

创建优惠券后，服务会把初始库存写入 Redis。调用：

```bash
curl -X POST http://localhost:8080/api/seckill/vouchers/1/orders \
  -H 'Authorization: Bearer <token>'
```

HTTP `202` 只代表请求已进入消息队列。最终订单由 Kafka 消费者写入数据库。
随后可调用 `GET /api/orders/requests/{requestId}` 查询落库结果；尚未消费时返回 `404`。

## 你本地重写时的顺序

1. 先只实现数据库版商家和订单，验证事务与唯一索引。
2. 给商家查询加 Cache Aside，观察第一次和第二次查询的差异。
3. 加空值缓存，再请求不存在的 ID，观察空值 Key 的 TTL。
4. 用 Redis Set 表达“一人一单”，把库存与集合检查放入一个 Lua 脚本。
5. 先同步调用数据库下单，再改成发送 Kafka 消息，比较接口耗时与失败语义。
6. 重复投递同一个 `requestId`，确认数据库只生成一条订单。
7. 最后实现 Bitmap、Set 交集、ZSet Feed 和限流，它们彼此独立，便于逐个练习。

## 生产化时仍需补充

- 短信服务与验证码风控；生产环境必须关闭 `HIGO_AUTH_EXPOSE_CODE`。
- Kafka 死信主题、告警与人工补偿任务。
- Redis 与数据库库存对账。
- 多实例下的缓存更新消息广播。
- 监控、链路追踪、容量测试和密钥管理。
