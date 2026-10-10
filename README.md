# 嗨Go · DianpingPlus 学习复现

一个可以在浏览器操作的本地生活学习项目：商家与优惠券、登录签到、探店笔记、关注和秒杀订单。独立实现参考项目的业务能力，使用 Java 21 / Spring Boot 3.5 / JPA，并非原仓库代码或接口的逐字复制。

## 本地运行

安装 JDK 21 和 Docker：

```bash
git clone https://github.com/paulljlj/project.git
cd project
docker compose up -d
./mvnw test
./mvnw spring-boot:run
```

打开 **http://localhost:8080**。先登录：填写手机号并点获取验证码，学习模式会显示验证码。添加商家 → 查看好店 → 发布优惠券 → 抢购 → 我的订单。也可以发布带图片的笔记、点赞评论、关注作者和签到。活动时间按 UTC 输入。

没有预置商家；你在页面创建的数据保存在 MySQL。上传文件保存在 `data/uploads`，会话保存在浏览器 sessionStorage。MySQL 的本地端口为 3307，Redis 为 6379，Kafka 为 9092。Docker 数据卷保留数据库和消息。

## 已实现

- 验证码一次性消费、登录注销、会话续期、个人资料、Bitmap 签到。
- 商家分类/店名搜索、GEO 附近查询、空值缓存、随机 TTL、热点逻辑过期与异步重建。
- 图片上传与归属校验、图文笔记、持久化点赞、评论、关注/共同关注、稳定游标 Feed。
- Lua 秒杀资格预留、一人一单、数据库条件扣库存、请求状态与订单归属校验。
- 数据库 Outbox 投递 Kafka、幂等消费、失败重试、死信记录与重放、Redis 库存重建。
- 模拟支付、幂等取消、30 分钟未支付自动取消与库存归还，行锁保护状态竞争。
- AOP + Lua 滑动窗口限流、Flyway 迁移、独立网页。

验证码和支付用于学习，未连接短信或支付平台。登录用户都可维护商家/优惠券，尚未实现运营管理员角色。数据库锁保证正确性但会串行化同一优惠券操作；本项目不声称达到生产秒杀吞吐量。

## 验证与学习

默认 `./mvnw test` 执行 H2 数据库基线测试；真实 MySQL/Redis/Kafka 测试需显式开启，见 [完整版本说明](docs/complete-version.md)。不要把集成测试指向个人开发数据库。

- [本地手写指南](docs/manual-build-guide.md)：从 `learning-start` 标签建立练习分支。
- [完整版本说明](docs/complete-version.md)：接口、可靠性边界、验证步骤。
- [DianpingPlus 参考对照](docs/reference-comparison.md)：功能差距、可靠性取舍与后续路线。
- [多实例秒杀压测](docs/multi-instance-load-guide.md)：两个应用实例下验证不超卖和一人一单。
- [第一阶段](docs/phase-1-core.md)：数据库核心。
- [第一版复现记录](docs/dianping-plus-reproduction.md)：历史设计，完整版本以当前文档为准。
- [学习路线收藏](LEARNING_ROADMAP.md)：之后的公开项目学习路线。
