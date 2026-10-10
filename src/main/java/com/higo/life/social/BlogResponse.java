package com.higo.life.social;

import com.higo.life.auth.User;
import java.time.LocalDateTime;

public record BlogResponse(
        Long id,
        Long userId,
        String author,
        String title,
        String content,
        java.util.List<String> images,
        int liked,
        boolean likedByMe,
        LocalDateTime createdAt
) {

    static BlogResponse from(Blog blog, User author, boolean likedByMe) {
        return new BlogResponse(
                blog.getId(), blog.getUserId(), author.getNickname(), blog.getTitle(), blog.getContent(),
                blog.getImages()==null || blog.getImages().isBlank()? java.util.List.of():java.util.List.of(blog.getImages().split(",")),
                blog.getLiked(), likedByMe, blog.getCreatedAt()
        );
    }
}
