package com.higo.life;

import com.fasterxml.jackson.databind.*;
import com.higo.life.auth.*;
import com.higo.life.social.*;
import com.higo.life.order.*;
import com.higo.life.shop.*;
import com.higo.life.voucher.*;
import com.higo.life.seckill.*;
import com.higo.life.cache.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

// Deliberately separate real MySQL/Redis/Kafka suite; never run against the default development database.
@SpringBootTest(properties={"higo.baseline.enabled=false", "spring.data.redis.database=15",
        "higo.kafka.order-topic=higo.integration.orders.v2", "higo.upload.dir=/tmp/higo-integration-uploads", "higo.auth.expose-code=true"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="HIGO_INTEGRATION",matches="true")
class FullStackIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired StringRedisTemplate redis;
    @Autowired UserRepository users; @Autowired ShopRepository shops; @Autowired VoucherRepository vouchers;
    @Autowired VoucherOrderRepository orders; @Autowired OrderRequestRepository requests;
    @Autowired BlogRepository blogs; @Autowired BlogLikeRepository likes; @Autowired BlogCommentRepository comments;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc; @Autowired OrderExpiryJob expiry;
    @Autowired FollowRepository follows; @Autowired OrderOutboxDispatcher dispatcher;
    @BeforeEach void clean() {
        assertThat(System.getenv("HIGO_DB_URL")).contains("higo_integration");
        comments.deleteAll();likes.deleteAll();blogs.deleteAll();follows.deleteAll();requests.deleteAll();orders.deleteAll();vouchers.deleteAll();shops.deleteAll();users.deleteAll();
        Set<String> keys=redis.keys("higo:*");if(keys!=null && !keys.isEmpty())redis.delete(keys);
    }
    record Session(String token,long id) {}
    Session login(String phone) throws Exception {
        JsonNode c=postJson("/api/auth/codes",Map.of("phone",phone),null,200);
        JsonNode s=postJson("/api/auth/sessions",Map.of("phone",phone,"code",c.get("code").asText()),null,200);
        return new Session(s.get("token").asText(),s.get("user").get("id").asLong());
    }
    JsonNode postJson(String path,Object value,String token,int expected) throws Exception {
        var req=post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(value));
        if(token!=null)req.header("Authorization","Bearer "+token);
        String body=mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isBlank()?json.nullNode():json.readTree(body);
    }
    String manager;
    long shop() throws Exception { manager=login("13900000000").token; return postJson("/api/shops",Map.of("name","咖啡测试","category","咖啡","address","广州","longitude",113.317,"latitude",23.083),manager,201).get("id").asLong(); }
    long voucher(long shop,int stock) throws Exception { return postJson("/api/vouchers",Map.of("shopId",shop,"title","限量咖啡","price",19.90,"stock",stock,"beginAt","2020-01-01T00:00:00","endAt","2099-01-01T00:00:00"),manager,201).get("id").asLong(); }
    @Test void otpIsSingleUseAndOrdersRequireAuthentication() throws Exception {
        var c=postJson("/api/auth/codes",Map.of("phone","13800000001"),null,200);
        var body=Map.of("phone","13800000001","code",c.get("code").asText());
        postJson("/api/auth/sessions",body,null,200);postJson("/api/auth/sessions",body,null,409);
        mvc.perform(get("/api/orders/mine")).andExpect(status().isUnauthorized());
        postJson("/api/auth/codes",Map.of("phone","13800000001"),null,409);
    }
    @Test void cacheInvalidationNullValuesGeoAndLogicalExpiry() throws Exception {
        long id=shop();
        mvc.perform(get("/api/shops/"+id)).andExpect(status().isOk());
        assertThat(redis.getExpire(CacheKeys.SHOP+id)).isBetween(1790L,2100L);
        mvc.perform(get("/api/shops/99999999")).andExpect(status().isNotFound());
        assertThat(redis.opsForValue().get(CacheKeys.SHOP+99999999)).isEqualTo("__NULL__");
        mvc.perform(get("/api/shops/search").param("category","咖啡").param("longitude","113.317").param("latitude","23.083")).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/shops/"+id+"/hot")).andExpect(status().isOk());
        String key=CacheKeys.SHOP+"hot:"+id;JsonNode entry=json.readTree(redis.opsForValue().get(key));
        ((com.fasterxml.jackson.databind.node.ObjectNode)entry).put("expiresAt",0);
        redis.opsForValue().set(key,json.writeValueAsString(entry));
        mvc.perform(get("/api/shops/"+id+"/hot")).andExpect(status().isOk());
        await(()-> { try { return json.readTree(redis.opsForValue().get(key)).get("expiresAt").asLong()>System.currentTimeMillis(); } catch(Exception ex) {return false;} });
        mvc.perform(put("/api/shops/"+id).header("Authorization","Bearer "+manager).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","新店名","category","咖啡","address","广州")))).andExpect(status().isOk());
        assertThat(redis.hasKey(key)).isFalse();assertThat(redis.hasKey(CacheKeys.SHOP+id)).isFalse();
    }
    @Test void profilesUploadsCommentsLikesFollowAndStableFeed() throws Exception {
        var author=login("13800000002");var reader=login("13800000003");
        mvc.perform(post("/api/follows/"+author.id).header("Authorization","Bearer "+reader.token)).andExpect(status().isNoContent());
        long blog=postJson("/api/blogs",Map.of("title","探店","content","值得一去"),author.token,201).get("id").asLong();
        mvc.perform(get("/api/blogs/feed/cursor").header("Authorization","Bearer "+reader.token)).andExpect(jsonPath("$.items[0].id").value(blog));
        mvc.perform(put("/api/blogs/"+blog+"/like").header("Authorization","Bearer "+reader.token)).andExpect(status().isNoContent());
        assertThat(likes.count()).isEqualTo(1);
        mvc.perform(get("/api/blogs/"+blog)).andExpect(jsonPath("$.liked").value(1));
        long comment=postJson("/api/blogs/"+blog+"/comments",Map.of("content","我也喜欢"),reader.token,201).get("id").asLong();
        mvc.perform(delete("/api/blogs/"+blog+"/comments/"+comment).header("Authorization","Bearer "+author.token)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/blogs/"+blog+"/comments/"+comment).header("Authorization","Bearer "+reader.token)).andExpect(status().isNoContent());
        var png=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",png);
        String uploaded=mvc.perform(multipart("/api/uploads").file(new MockMultipartFile("file","image.png","image/png",png.toByteArray())).header("Authorization","Bearer "+author.token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String url=json.readTree(uploaded).get("url").asText();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(delete(url).header("Authorization","Bearer "+reader.token)).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/uploads").file(new MockMultipartFile("file","bad.png","image/png","fake".getBytes())).header("Authorization","Bearer "+author.token)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/users/me").header("Authorization","Bearer "+author.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nickname","新昵称","bio","咖啡爱好者","icon",url)))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+author.token)).andExpect(jsonPath("$.nickname").value("新昵称"));
        mvc.perform(post("/api/sign-ins/today").header("Authorization","Bearer "+author.token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/sign-ins/streak").header("Authorization","Bearer "+author.token)).andExpect(jsonPath("$.consecutiveDays").value(1));
    }
    @Test void asyncRequestOwnershipIdempotencyAndCancellation() throws Exception {
        var a=login("13800000004");var b=login("13800000005");long id=voucher(shop(),2);
        String request=postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),a.token,202).get("requestId").asText();
        await(()->orders.findByRequestId(request).isPresent());
        long order=orders.findByRequestId(request).orElseThrow().getId();
        mvc.perform(get("/api/orders/"+order).header("Authorization","Bearer "+b.token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/seckill/requests/"+request).header("Authorization","Bearer "+a.token)).andExpect(jsonPath("$.status").value("COMPLETED"));
        postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),a.token,409);
        mvc.perform(post("/api/orders/"+order+"/cancel").header("Authorization","Bearer "+a.token)).andExpect(status().isOk());
        mvc.perform(post("/api/orders/"+order+"/cancel").header("Authorization","Bearer "+a.token)).andExpect(status().isOk());
        assertThat(vouchers.findById(id).orElseThrow().getStock()).isEqualTo(2);
        assertThat(redis.opsForValue().get(CacheKeys.SECKILL_STOCK+id)).isEqualTo("2");
        mvc.perform(post("/api/orders/"+order+"/pay").header("Authorization","Bearer "+a.token)).andExpect(status().isConflict());
    }
    @Test void concurrentBuyersCannotOversellAndRedisRecoveryPreservesReservations() throws Exception {
        long id=voucher(shop(),3);var sessions=new ArrayList<Session>();
        for(int i=10;i<22;i++)sessions.add(login("138000000"+i));
        var pool=Executors.newFixedThreadPool(12);
        try {
            var futures=new ArrayList<Future<Integer>>();
            for(var s:sessions)futures.add(pool.submit(()->mvc.perform(post("/api/seckill/vouchers/"+id+"/orders").header("Authorization","Bearer "+s.token)).andReturn().getResponse().getStatus()));
            int accepted=0;for(var f:futures){int status=f.get(15,TimeUnit.SECONDS);assertThat(status).isIn(202,409);if(status==202)accepted++;}
            assertThat(accepted).isEqualTo(3);
            await(()->orders.findByVoucherId(id).size()==3);
            assertThat(vouchers.findById(id).orElseThrow().getStock()).isZero();
            redis.delete(CacheKeys.SECKILL_STOCK+id);
            postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),login("13800000030").token,409);
            assertThat(redis.opsForValue().get(CacheKeys.SECKILL_STOCK+id)).isEqualTo("0");
        } finally {pool.shutdownNow();}
    }
    @Test void durableOutboxAndDeadLetterReplay() throws Exception {
        var a=login("13800000040");long id=voucher(shop(),1);
        // A persisted request can survive a worker restart; dispatch works without the HTTP handler.
        String request=UUID.randomUUID().toString();requests.saveAndFlush(new OrderRequest(request,a.id,id));
        dispatcher.dispatch();await(()->orders.findByRequestId(request).isPresent());
        assertThat(requests.findById(request).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        // Force a business failure (zero DB stock): retries go to DLT and the request remains inspectable.
        var b=login("13800000041");String bad=UUID.randomUUID().toString();requests.saveAndFlush(new OrderRequest(bad,b.id,id));
        dispatcher.dispatch();await(()->requests.findById(bad).orElseThrow().getStatus().equals("FAILED"));
        assertThat(orders.count()).isEqualTo(1);
        postJson("/api/seckill/requests/"+bad+"/retry",Map.of(),a.token,404);
        jdbc.update("update vouchers set stock=1 where id=?",id);
        postJson("/api/seckill/requests/"+bad+"/retry",Map.of(),b.token,202);
        await(()->requests.findById(bad).orElseThrow().getStatus().equals("COMPLETED"));
        assertThat(orders.count()).isEqualTo(2);
    }
    @Test void unpaidOrdersExpireButPaidOrdersStayPaid() throws Exception {
        long id=voucher(shop(),2);var a=login("13800000060");var b=login("13800000061");
        String first=postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),a.token,202).get("requestId").asText();
        String second=postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),b.token,202).get("requestId").asText();
        await(()->orders.findByVoucherId(id).size()==2);
        long paid=orders.findByRequestId(second).orElseThrow().getId();
        mvc.perform(post("/api/orders/"+paid+"/pay").header("Authorization","Bearer "+b.token)).andExpect(status().isOk());
        jdbc.update("update voucher_orders set created_at=? where voucher_id=?",java.sql.Timestamp.valueOf(LocalDateTime.now().minusHours(1)),id);
        expiry.expire();expiry.expire();
        assertThat(orders.findByRequestId(first).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.findByRequestId(second).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(vouchers.findById(id).orElseThrow().getStock()).isEqualTo(1);
    }
    @Test void userWindowRejectsSixthRequest() throws Exception {
        var a=login("13800000050");long id=voucher(shop(),0);
        for(int i=0;i<5;i++)postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),a.token,409);
        postJson("/api/seckill/vouchers/"+id+"/orders",Map.of(),a.token,429);
    }
    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(!condition.getAsBoolean() && System.nanoTime()<deadline)Thread.sleep(100);
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
