package com.higo.life.social;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface BlogLikeRepository extends JpaRepository<BlogLike,Long> {
    Optional<BlogLike> findByBlogIdAndUserId(Long blogId,Long userId);
    boolean existsByBlogIdAndUserId(Long blogId,Long userId);
    List<BlogLike> findByBlogIdOrderByCreatedAtAscIdAsc(Long blogId,Pageable page);
}
