package com.higo.life.seckill;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface OrderRequestRepository extends JpaRepository<OrderRequest,String> {
    boolean existsByUserIdAndVoucherId(Long userId,Long voucherId);
    List<OrderRequest> findTop20BySentFalseAndStatusOrderByCreatedAtAsc(String status);
    List<OrderRequest> findByVoucherId(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from OrderRequest r where r.id=:id")
    Optional<OrderRequest> lockById(@Param("id") String id);
}
