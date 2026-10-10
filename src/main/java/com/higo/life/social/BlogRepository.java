package com.higo.life.social;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Blog b where b.id=:id")
    java.util.Optional<Blog> lockById(@Param("id") Long id);
    List<Blog> findByUserIdOrderByIdDesc(Long userId,Pageable page);
    @Query("select b from Blog b where b.userId in (select f.targetUserId from Follow f where f.userId=:userId) and b.id<:before order by b.id desc")
    List<Blog> followingFeed(@Param("userId") Long userId,@Param("before") Long before,Pageable page);
    List<Blog> findAllByOrderByLikedDescCreatedAtDesc(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Blog b set b.liked = b.liked + :delta where b.id = :id and b.liked + :delta >= 0")
    int adjustLikes(@Param("id") Long id, @Param("delta") int delta);
}
