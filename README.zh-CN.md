# 嗨go生活学习项目

这是一个从零实现的本地生活与高并发优惠券系统，用来分阶段学习 Spring Boot、JPA、MySQL、Redis、Lua 和 Kafka。

项目不会直接复制公开仓库代码。每个阶段先建立一个可以运行和验证的基线，再引入新技术解决明确的问题。

## 当前阶段

第一阶段“数据库交易核心”已经完成：

- 商家创建与查询
- 优惠券创建与查询
- 用户下单与订单查询
- 数据库原子扣减库存
- 一人一单唯一约束
- Flyway 数据库迁移
- HTTP 集成测试

详细说明见 [第一阶段学习笔记](docs/phase-1-core.md)。

## 技术栈

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- MySQL 8.4
- Flyway
- Maven Wrapper
- JUnit 5、MockMvc、H2

## 快速开始

准备 JDK 21 和 Docker，然后执行：

```bash
docker compose up -d mysql
./mvnw test
./mvnw spring-boot:run
```

应用默认地址为 `http://localhost:8080`，MySQL 使用宿主机端口 `3307`。

## 核心接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/shops` | 创建商家 |
| `GET` | `/api/shops` | 查询商家列表 |
| `GET` | `/api/shops/{id}` | 查询商家详情 |
| `POST` | `/api/vouchers` | 创建优惠券 |
| `GET` | `/api/shops/{shopId}/vouchers` | 查询商家优惠券 |
| `POST` | `/api/orders` | 下单 |
| `GET` | `/api/orders/{id}` | 查询订单 |

## 学习路线

1. ✅ MySQL 同步交易核心
2. Redis Cache Aside 与缓存治理
3. Redis + Lua 秒杀资格预检
4. Kafka 异步创建订单
5. 消费幂等、失败重试与补偿
6. 支付、关单和乐观锁
7. 并发测试与性能对比

完整路线和公开参考项目见 [LEARNING_ROADMAP.md](LEARNING_ROADMAP.md)。

