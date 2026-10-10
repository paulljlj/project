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
2. ✅ Redis：商家缓存、登录状态、GEO、签到和社交数据结构。
3. ✅ Lua：原子校验库存、一人一单和滑动窗口限流。
4. ✅ Kafka：异步创建订单、请求号幂等、持久 Outbox 与死信重放。
5. ✅ MySQL：条件扣减和唯一索引兜底。
6. ✅ 缓存治理：空值缓存、随机 TTL 和逻辑过期重建。
7. ✅ 并发压测：验证多实例下不超卖、不重复下单和消息重放。

## 当前进度

- 完整学习版：网页、登录资料、商家优惠券、图文笔记、关注/点赞/评论、秒杀请求、可靠异步下单、模拟支付/取消与超时关单。
- H2 基线测试与真实 MySQL/Redis/Kafka 功能测试；并发用例为 12 个用户争抢 3 份库存。
- 浏览器冒烟覆盖登录、商家优惠券、抢购取消、笔记点赞评论与手机布局。
- 零依赖多实例压力脚本覆盖跨实例一人一单、库存边界和最终订单唯一性。
- Redis 分布式业务编号、后端镜像与真实短信/支付接入仍可作为之后的扩展练习。

下一步由你在本地从 `learning-start` 手写，对照 [手写指南](docs/manual-build-guide.md)。做到阶段七时使用 [多实例秒杀压测](docs/multi-instance-load-guide.md) 验收；与参考仓库的差异见 [参考对照](docs/reference-comparison.md)。完成后返回此收藏，再开始下一个公开项目。

收藏标签：`#公开项目学习-嗨go-01`。完整版本见 [说明](docs/complete-version.md)，Git 标签 `dianping-plus-v2`。
