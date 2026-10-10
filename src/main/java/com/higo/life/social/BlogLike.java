package com.higo.life.social;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="blog_likes", uniqueConstraints=@UniqueConstraint(columnNames={"blog_id","user_id"}))
public class BlogLike {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="blog_id",nullable=false) private Long blogId;
    @Column(name="user_id",nullable=false) private Long userId;

    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    protected BlogLike() {}
    public BlogLike(Long blogId,Long userId) { this.blogId=blogId;this.userId=userId; }
    @PrePersist void create() { createdAt=LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getBlogId() { return blogId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}
