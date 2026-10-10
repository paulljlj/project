package com.higo.life.social;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="blog_comments")
public class BlogComment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="blog_id",nullable=false) private Long blogId;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=1000) private String content;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    protected BlogComment() {}
    public BlogComment(Long blogId,Long userId,String content) { this.blogId=blogId;this.userId=userId;this.content=content; }
    @PrePersist void create() { createdAt=LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getBlogId() { return blogId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getContent() { return content; }
}
