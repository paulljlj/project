package com.higo.life.voucher;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findByShopIdOrderByIdAsc(Long shopId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Voucher v set v.stock = v.stock - 1 where v.id = :id and v.stock > 0")
    int decrementStock(@Param("id") Long id);
}

