package com.higo.life;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higo.life.order.VoucherOrderRepository;
import com.higo.life.order.OrderService;
import com.higo.life.shop.ShopRepository;
import com.higo.life.voucher.VoucherRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    VoucherOrderRepository orderRepository;

    @Autowired
    OrderService orderService;

    @Autowired
    VoucherRepository voucherRepository;

    @Autowired
    ShopRepository shopRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        voucherRepository.deleteAll();
        shopRepository.deleteAll();
    }

    @Test
    void completesOrderFlowAndProtectsStockAndOneOrderPerUser() throws Exception {
        long shopId = createShop();
        long voucherId = createVoucher(shopId, 1);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", 1001, "voucherId", voucherId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(29.90));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", 1001, "voucherId", voucherId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("同一用户不能重复购买同一张优惠券"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", 1002, "voucherId", voucherId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("优惠券库存不足"));

        mockMvc.perform(get("/api/shops/{shopId}/vouchers", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(0));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void consumesTheSameAsyncRequestOnlyOnce() throws Exception {
        long shopId = createShop();
        long voucherId = createVoucher(shopId, 1);

        var first = orderService.placeReserved("request-001", 2001L, voucherId);
        var replay = orderService.placeReserved("request-001", 2001L, voucherId);

        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(voucherRepository.findById(voucherId).orElseThrow().getStock()).isZero();
    }

    private long createShop() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/shops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "海珠咖啡",
                                "category", "咖啡",
                                "address", "广州市海珠区学习路 1 号"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private long createVoucher(long shopId, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "shopId", shopId,
                                "title", "咖啡体验券",
                                "description", "学习项目测试券",
                                "price", 29.90,
                                "stock", stock,
                                "beginAt", LocalDateTime.of(2020, 1, 1, 0, 0).toString(),
                                "endAt", LocalDateTime.of(2099, 1, 1, 0, 0).toString()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asLong();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}

