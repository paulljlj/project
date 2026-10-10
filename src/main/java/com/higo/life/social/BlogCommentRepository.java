package com.higo.life.social;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface BlogCommentRepository extends JpaRepository<BlogComment,Long> {
    List<BlogComment> findByBlogIdOrderByIdAsc(Long blogId,Pageable page);
}
