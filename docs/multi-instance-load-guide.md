# 多实例秒杀压测指南

这个练习验证的不是单个方法能否运行，而是两个应用实例共享 MySQL、Redis 和 Kafka 时，系统仍然不超卖、不重复下单，并能把已受理请求最终处理完成。

## 自动验证

先启动基础设施并构建一次：

```bash
docker compose up -d --wait
./mvnw -DskipTests package
```

打开两个终端，使用同一个 JAR 启动两个实例：

```bash
SERVER_PORT=8080 java -jar target/higo-life-0.1.0-SNAPSHOT.jar
```

```bash
SERVER_PORT=8081 java -jar target/higo-life-0.1.0-SNAPSHOT.jar
```

第三个终端运行零依赖压测脚本：

```bash
HIGO_BASE_URLS=http://127.0.0.1:8080,http://127.0.0.1:8081 \
HIGO_BUYERS=24 \
HIGO_STOCK=5 \
node scripts/seckill-load.cjs
```

脚本会创建独立学习数据，执行两组检查：

1. 同一用户同时请求两个实例，必须只有一次被受理并只生成一张订单。
2. 24 个用户轮询请求两个实例、争抢 5 份库存，必须正好 5 个 `202`、19 个 `409`，最终得到 5 个不同订单。

成功输出包含 `"result": "PASS"`。脚本依赖学习模式返回验证码；如果关闭了 `higo.auth.expose-code`，请不要在真实短信环境运行它。

可调参数：

| 环境变量 | 默认值 | 用途 |
| --- | ---: | --- |
| `HIGO_BASE_URLS` | `http://127.0.0.1:8080` | 逗号分隔的应用实例地址 |
| `HIGO_BUYERS` | `24` | 并发购买用户数，2～200 |
| `HIGO_STOCK` | `5` | 库存，必须小于用户数 |
| `HIGO_COMPLETION_TIMEOUT_MS` | `60000` | 等待 Outbox 与 Kafka 完成的最长时间 |
| `HIGO_REQUEST_TIMEOUT_MS` | `10000` | 单次 HTTP 请求超时 |

## 手工观察

压测时同时观察三层状态：

```bash
docker compose exec redis redis-cli --scan --pattern 'higo:seckill:*'
docker compose exec mysql mysql -uhigo -phigo_dev higo -e 'select voucher_id,status,count(*) from voucher_orders group by voucher_id,status;'
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --all-groups --describe
```

重点解释以下现象：

- 两个实例都可能扫描到尚未发送的 Outbox 请求；即使消息重复，订单唯一约束仍应保持结果不变。
- Redis 库存是快速预留值，MySQL 库存是最终事实，二者短暂不同不等于超卖。
- HTTP `202` 只代表请求已持久化受理，脚本还会轮询到 `COMPLETED` 才判定成功。

## 故障练习

1. 请求被受理后立即停止一个实例，确认另一个实例继续派发和消费。
2. 暂停 Kafka，再发请求；恢复 Kafka 后确认 Outbox 继续投递。
3. 删除 Redis 秒杀库存 Key，再请求一次，观察数据库锁保护下的库存重建。
4. 把消费者故意改成抛异常，观察重试、DLT、`FAILED` 和重放流程；实验后恢复代码再提交。

不要把压测指向生产环境。脚本会创建用户、商家、优惠券和订单数据。
