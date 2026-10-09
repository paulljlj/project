package com.higo.life.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, Long> {

    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);

    Optional<VoucherOrder> findByUserIdAndVoucherId(Long userId, Long voucherId);

    Optional<VoucherOrder> findByRequestId(String requestId);
}
