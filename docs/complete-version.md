# 完整学习版本

## 如何开始

参考 README 启动全部三个基础服务与应用。首页不需要额外前端构建。先在页面登录，添加商家，发布正在进行的优惠券，再测试抢购；用另一个浏览器上下文测试关注、评论与订单隔离。

## 新增接口

所有写入操作需要 Bearer token。商家与笔记读取公开；订单、请求状态与上传删除仅允许本人。

| 方法 | 接口 | 用途 |
| --- | --- | --- |
| GET / PUT | `/api/users/{id}` / `/api/users/me` | 用户公开资料 / 修改本人资料 |
| GET | `/api/shops/categories`、`/api/shops/by-name?name=咖啡&page=0` | 分类和名称搜索 |
| GET | `/api/shops/{id}/hot` | 逻辑过期热点缓存演示 |
| POST / GET / DELETE | `/api/uploads` / `/api/uploads/{filename}` | 上传图片 / 读取 / 本人删除 |
| GET | `/api/blogs/{id}`、`/api/blogs/of-user/{id}` | 笔记详情与作者笔记 |
| GET / POST / DELETE | `/api/blogs/{id}/comments`、`/api/blogs/{id}/comments/{commentId}` | 评论列表 / 发布 / 本人删除 |
| GET | `/api/blogs/feed/cursor?beforeId=9223372036854775807` | 稳定 ID 游标 Feed |
| GET | `/api/seckill/requests/{requestId}` | 本人请求状态 |
| POST | `/api/seckill/requests/{requestId}/retry` | 本人 FAILED 请求重投 |
| POST | `/api/seckill/vouchers/{id}/reconcile` | 根据数据库重建库存与购买集合 |
| GET | `/api/orders/mine` | 本人订单 |
| POST | `/api/orders/{id}/pay`、`/api/orders/{id}/cancel` | 模拟支付 / 取消 |

默认禁用历史 `/api/orders` 同步下单入口，以免绕开秒杀预留。仅 H2 学习基线测试启用 `higo.baseline.enabled=true`。

## 请求、事务和故障

秒杀先锁定优惠券行，执行 Redis Lua 预留，再在同一数据库事务保存 `order_requests`。返回 202 只表示已受理。后台 Outbox 等 Kafka 确认后标记 sent；超时会重发，不立即归还库存，因为消息可能已经到达。消费者按 requestId 与用户/优惠券唯一约束去重，数据库条件扣库存后生成订单并更新 COMPLETED。

消费失败重试后送入 `.DLT`，死信消费者记录 FAILED。请求保留库存预留，修复原因后可重投。取消 CREATED 订单会归还数据库库存，并在提交后更新 Redis；PAID 不允许取消。每分钟扫描一批超过 30 分钟的待支付订单，支付和取消共享订单行锁。配置 `higo.orders.timeout-minutes` 修改时长，<=0 可关闭。

Redis 库存丢失时，以数据库库存减去尚未完成的请求重建，并重建购买集合；重建和预留/消费共用优惠券行锁。取消后仍保留一人一单记录，不允许同一用户再次购买同一券。

**边界：** Redis 与 MySQL 不是原子提交。进程在 Lua 成功、数据库提交之前崩溃，或提交后 Redis 投影失败，仍需调用 reconcile 修复。普通 Cache Aside 存在并发回填旧值窗口，依靠有限 TTL 收敛；热点重建采用令牌锁及 CAS 避免删除后异步任务回写。GEO/历史 Redis Feed 投影不是持久事务消息；稳定 Feed、点赞和关注以 MySQL 为事实来源。单节点 Docker 不提供高可用。

## 真实服务测试

先 `docker compose up -d`。创建专用数据库（仅使用 Compose 的开发凭据）：

```bash
docker compose exec mysql mysql -uroot -proot_dev -e "CREATE DATABASE IF NOT EXISTS higo_integration; GRANT ALL ON higo_integration.* TO 'higo'@'%';"
HIGO_INTEGRATION=true HIGO_DB_URL='jdbc:mysql://localhost:3307/higo_integration?allowPublicKeyRetrieval=true&useSSL=false' ./mvnw -Dtest=FullStackIntegrationTest test
```

测试会清空 `higo_integration`，使用 Redis DB 15 和独立 Kafka topic，不能和另一份集成测试并行运行。测试包括验证码复用、上传归属、评论权限、缓存、GEO、点赞/Feed、12 用户争抢 3 份库存、Outbox 和死信、请求与订单隔离、幂等取消和滑动窗口限流。

浏览器冒烟脚本见 `scripts/browser-smoke.cjs`。安装 Playwright 后，启动应用，再运行 `PLAYWRIGHT_MODULE=playwright BROWSER_PATH=/path/to/chromium node scripts/browser-smoke.cjs`；默认地址 localhost:8080，可用 `HIGO_BASE_URL` 修改。脚本创建学习数据，不清空数据库。

## 手写顺序

从 `learning-start` 开始：数据库核心 → Cache Aside → 验证码与 Bitmap → GEO → Lua → Kafka 与幂等 → Outbox/死信/重建 → 订单状态竞争 → 社交持久化 → 图片与页面。先实现和观察失败，再对照完整版本。页面最后写，避免同时调试太多层。
