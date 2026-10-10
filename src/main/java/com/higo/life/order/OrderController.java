package com.higo.life.order;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final com.higo.life.auth.CurrentUser current;
    private final boolean baseline;

    public OrderController(OrderService orderService,com.higo.life.auth.CurrentUser current,@org.springframework.beans.factory.annotation.Value("${higo.baseline.enabled:false}") boolean baseline) {
        this.orderService = orderService; this.current=current; this.baseline=baseline;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        if(!baseline) throw new com.higo.life.support.InvalidRequestException("请使用秒杀接口下单");
        VoucherOrder order = orderService.place(request.userId(), request.voucherId());
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/mine") public java.util.List<OrderResponse> mine() { return orderService.mine(current.require().id()).stream().map(OrderResponse::from).toList(); }
    @PostMapping("/{id}/pay") public OrderResponse pay(@PathVariable Long id) { var o=orderService.get(id); own(o); return OrderResponse.from(orderService.transition(id,current.require().id(),true)); }
    @PostMapping("/{id}/cancel") public OrderResponse cancel(@PathVariable Long id) { var o=orderService.get(id); own(o); return OrderResponse.from(orderService.transition(id,current.require().id(),false)); }
    private void own(VoucherOrder order) { if(!baseline && !order.getUserId().equals(current.require().id())) throw new com.higo.life.support.NotFoundException("订单不存在"); }
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        VoucherOrder order=orderService.get(id); own(order); return OrderResponse.from(order);
    }

    @GetMapping("/requests/{requestId}")
    public OrderResponse getByRequestId(@PathVariable String requestId) {
        VoucherOrder order=orderService.getByRequestId(requestId); own(order); return OrderResponse.from(order);
    }
}

