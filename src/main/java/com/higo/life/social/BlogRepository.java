package com.higo.life.social;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    List<Blog> findAllByOrderByLikedDescCreatedAtDesc(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Blog b set b.liked = b.liked + :delta where b.id = :id and b.liked + :delta >= 0")
    int adjustLikes(@Param("id") Long id, @Param("delta") int delta);
}
