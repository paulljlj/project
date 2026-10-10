package com.higo.life.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "voucher_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_user_voucher",
                columnNames = {"user_id", "voucher_id"}
        )
)
public class VoucherOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "request_id", unique = true, length = 36)
    private String requestId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected VoucherOrder() {
    }

    public VoucherOrder(Long userId, Long voucherId, BigDecimal amount) {
        this(null, userId, voucherId, amount);
    }

    public VoucherOrder(String requestId, Long userId, Long voucherId, BigDecimal amount) {
        this.requestId = requestId;
        this.userId = userId;
        this.voucherId = voucherId;
        this.amount = amount;
        this.status = OrderStatus.CREATED;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void pay() { status=OrderStatus.PAID; }
    public void cancel() { status=OrderStatus.CANCELLED; }
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public String getRequestId() {
        return requestId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

