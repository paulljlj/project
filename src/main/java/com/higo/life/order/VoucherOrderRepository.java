package com.higo.life.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, Long> {

    java.util.List<VoucherOrder> findTop100ByStatusAndCreatedAtBeforeOrderByIdAsc(OrderStatus status, java.time.LocalDateTime before);
    java.util.List<VoucherOrder> findByVoucherId(Long voucherId);
    java.util.List<VoucherOrder> findByUserIdOrderByIdDesc(Long userId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from VoucherOrder o where o.id=:id")
    Optional<VoucherOrder> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);

    Optional<VoucherOrder> findByUserIdAndVoucherId(Long userId, Long voucherId);

    Optional<VoucherOrder> findByRequestId(String requestId);
}
