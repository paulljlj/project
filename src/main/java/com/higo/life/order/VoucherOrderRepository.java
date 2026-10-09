package com.higo.life.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, Long> {

    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
}

