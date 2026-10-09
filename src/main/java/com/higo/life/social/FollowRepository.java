package com.higo.life.social;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByUserIdAndTargetUserId(Long userId, Long targetUserId);

    long deleteByUserIdAndTargetUserId(Long userId, Long targetUserId);

    List<Follow> findByTargetUserId(Long targetUserId);
}
