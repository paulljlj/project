# 公开项目学习路线

收藏标签：`#公开项目学习-嗨go-01`

保存日期：2026-10-09

## 当前学习项目

以“嗨go生活”为目标，学习本地生活与高并发优惠券秒杀系统。

目前没有找到名称完全一致的公开仓库。该项目很可能是在“黑马点评”基础上改名，并增加 Kafka 异步下单等功能后的版本。

## 公开参考项目

1. [Dylan4real/dianpingPlus](https://github.com/Dylan4real/dianpingPlus)
   - 技术栈最接近目标：Spring Boot、MySQL、Redis、Lua、Kafka、Redisson、Docker。
   - 主要用于参考功能和系统结构。
2. [john10hyh/hmdp-local-life-promotion](https://github.com/john10hyh/hmdp-local-life-promotion)
   - 提供较清楚的文档、自动化测试和 Docker Compose。
   - 主要用于参考测试方法、部署方式和分阶段开发流程。
3. [KNeegcyao/dianping](https://github.com/KNeegcyao/dianping)
   - 较完整的“黑马点评”学习版本。
   - 主要用于理解原始业务和 Redis 应用。
4. [potatoHerooo/huili-life](https://github.com/potatoHerooo/huili-life)
   - 项目描述与目标项目接近，并使用 Kafka。
   - 仓库文档和结构相对杂乱，只作为补充参考。

这些仓库没有发现清晰的项目级开源许可证。学习时参考需求和架构，在本仓库独立实现，不直接复制后重新发布。

## 实现顺序

1. ✅ Spring Boot、MySQL：商家、优惠券、库存和订单基础功能。
2. Redis：商家缓存、登录状态和优惠券库存。
3. Lua：原子校验库存、一人一单和扣减 Redis 库存。
4. Kafka：异步创建订单、消费幂等和失败处理。
5. MySQL：条件扣减、唯一索引和乐观锁兜底。
6. 缓存治理：空值缓存、逻辑过期、随机 TTL。
7. 并发测试：验证不超卖、不重复下单和消息幂等。

## 当前进度

- 第一阶段“数据库交易核心”已完成。
- 已实现商家、优惠券和订单 REST API。
- 已使用数据库条件更新防止库存扣成负数。
- 已使用唯一约束防止同一用户重复购买。
- H2 自动化集成测试和真实 MySQL 8.4 接口验收均已通过。
- 学习说明见 [`docs/phase-1-core.md`](docs/phase-1-core.md)。
- 下一阶段：Redis Cache Aside、空值缓存和缓存一致性。

## 后续项目队列

1. 完成“嗨go生活”核心版和增强版。
2. 学习缓存预拉取、断点重传和失败重试系统。
3. 学习 RAG、Tool Calling、MCP 和 ReAct Agent 项目。

## 返回口令

在当前聊天中说“继续 `#公开项目学习-嗨go-01`”，即可从该项目继续；完成后再按“后续项目队列”选择下一个公开项目。
