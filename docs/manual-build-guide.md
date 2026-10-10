# 本地手写指南

这份指南用于你在自己的电脑上重新实现项目。不要一次复制最终代码；每完成一阶段先运行测试、观察 Redis/MySQL，再进入下一阶段。

## 准备基线

克隆仓库后，从只有数据库核心的提交建立练习分支：

```bash
git clone https://github.com/paulljlj/project.git
cd project
git switch -c my-dianping-learning c54e441
docker compose up -d mysql
./mvnw test
```

这个提交只有商家、优惠券、同步下单和数据库防超卖。后续每一阶段建议单独提交，方便比较代码变化。

## 阶段一：商家缓存

目标：理解 Cache Aside，而不是先追求通用缓存工具。

1. 加入 `spring-boot-starter-data-redis`。
2. 在配置文件中加入 Redis 地址。
3. 修改 `ShopService.get`：先读 `higo:shop:{id}`，未命中再查数据库并回填。
4. 更新商家时先提交数据库事务，再删除缓存。
5. 为不存在的商家写入两分钟空值标记。
6. 给正常缓存的 30 分钟 TTL 增加 0～5 分钟随机值。

检查点：

```bash
redis-cli GET higo:shop:1
redis-cli TTL higo:shop:1
redis-cli GET higo:shop:999999
```

思考题：为什么更新后选择删除缓存，而不是直接写入新值？数据库事务回滚时会发生什么？

## 阶段二：验证码登录与签到

目标：理解无状态 HTTP 如何借助 Redis 保存短期状态。

1. 验证码写入 `higo:auth:code:{phone}`，有效期五分钟。
2. 使用 `SETNX higo:auth:code-rate:{phone}` 限制一分钟内重复发送。
3. 登录成功后生成随机 token，把最小用户信息写入 `higo:session:{token}`。
4. 拦截器从 `Authorization: Bearer ...` 恢复用户，并刷新会话 TTL。
5. 使用 `SETBIT` 保存当月签到，使用 `BITFIELD` 读取截至当天的位图并计算连续天数。

检查点：重复请求验证码应返回 `409`；访问受保护接口时不带 token 应返回 `401`。

## 阶段三：Lua 秒杀资格预检

目标：把多个 Redis 命令组成一个不可分割的操作。

Lua 脚本只做三件事：

1. 检查库存 Key 是否大于零。
2. 检查用户是否已在购买 Set 中。
3. 扣减库存并把用户加入 Set。

返回码建议固定为：`0=成功`、`1=库存不足`、`2=重复购买`、`3=库存未初始化`。Java 层负责把返回码转换成清楚的业务错误。

并发测试前先验证补偿脚本：先模拟数据库事务回滚，确认库存恢复、用户从购买集合移除。注意 Kafka 发送超时不代表发送失败，不能立即补偿；最终版本使用数据库 Outbox 保留并重试请求。

## 阶段四：Kafka 异步落库

目标：区分“资格预留成功”和“订单最终创建成功”。

1. Lua 成功后生成 `requestId`。
2. 先在数据库事务中保存请求，再由 Outbox 后台发送 `{requestId,userId,voucherId}` 到 Kafka。
3. 接口返回 HTTP `202 ACCEPTED`。
4. 消费者先按 `requestId` 查询；已存在则直接返回。
5. 使用 `UPDATE ... SET stock=stock-1 WHERE stock>0` 扣减数据库库存。
6. 保存订单，并保留 `(user_id,voucher_id)` 唯一约束。

检查点：手工重复发送同一个消息，数据库订单数必须仍为一；两个不同用户争抢最后一份库存时最多成功一个。

## 阶段五：GEO、关注和 Feed

目标：根据访问方式选择 Redis 数据结构。

- GEO：成员是商家 ID，坐标是经纬度，查询 5 公里范围并按距离排序。
- Set：`higo:following:{userId}` 保存关注关系；共同关注使用 `SINTER`。
- ZSet：`higo:blog:likes:{blogId}` 保存点赞时间；Feed 用发布时间作为 score。
- 推模式：作者发布笔记时，把笔记 ID 写入每个粉丝的 Feed ZSet。

思考题：拥有百万粉丝的作者是否还适合纯推模式？可以怎样改成推拉结合？

## 阶段六：AOP 滑动窗口限流

目标：把横切逻辑封装成注解，同时保持计数原子性。

1. 注解保存窗口秒数、配额和 USER/IP/METHOD 维度。
2. AOP 根据当前请求生成限流 Key。
3. Lua 删除窗口外成员，检查 `ZCARD`，通过后写入唯一成员并更新过期时间。
4. 超限返回 HTTP `429`。

不要只用毫秒时间戳作为 ZSet member；同一毫秒的请求会互相覆盖，应附加 UUID。

## 完整版本扩展

继续实现逻辑过期、持久化点赞/评论、图片上传、稳定 Feed、请求状态、死信重放、Redis 重建、模拟支付与超时关单。具体边界和验证见 [完整版本说明](complete-version.md)。

## 最终对照

完成每个阶段后，可与 `main` 分支对应包对照：

- `cache`：缓存治理
- `auth`、`signin`：登录与 Bitmap
- `seckill`：Lua 和 Kafka
- `social`：关注、点赞与 Feed
- `ratelimit`：AOP 滑动窗口

先描述自己的实现和测试结果，再看最终代码找差异。重点解释每个数据结构、事务边界和失败补偿，不以代码行数是否相同作为完成标准。
