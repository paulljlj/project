package com.higo.life.social;

import com.higo.life.auth.AuthenticatedUser;
import com.higo.life.auth.CurrentUser;
import com.higo.life.auth.User;
import com.higo.life.auth.UserRepository;
import com.higo.life.cache.CacheKeys;
import com.higo.life.support.NotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {

    private final BlogRepository blogRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final CurrentUser currentUser;

    public BlogService(
            BlogRepository blogRepository,
            FollowRepository followRepository,
            UserRepository userRepository,
            StringRedisTemplate redis,
            CurrentUser currentUser
    ) {
        this.blogRepository = blogRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.redis = redis;
        this.currentUser = currentUser;
    }

    @Transactional
    public BlogResponse publish(CreateBlogRequest request) {
        AuthenticatedUser author = currentUser.require();
        Blog blog = blogRepository.save(new Blog(author.id(), request.title(), request.content()));
        long now = System.currentTimeMillis();
        for (Follow follower : followRepository.findByTargetUserId(author.id())) {
            redis.opsForZSet().add(CacheKeys.FEED + follower.getUserId(), blog.getId().toString(), now);
        }
        return response(blog);
    }

    @Transactional
    public void toggleLike(Long blogId) {
        Long userId = currentUser.require().id();
        if (!blogRepository.existsById(blogId)) {
            throw new NotFoundException("笔记不存在: " + blogId);
        }
        String key = CacheKeys.BLOG_LIKES + blogId;
        Double score = redis.opsForZSet().score(key, userId.toString());
        int delta = score == null ? 1 : -1;
        blogRepository.adjustLikes(blogId, delta);
        if (delta > 0) {
            redis.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
        } else {
            redis.opsForZSet().remove(key, userId.toString());
        }
    }

    @Transactional(readOnly = true)
    public List<BlogResponse> hot(int page) {
        return blogRepository.findAllByOrderByLikedDescCreatedAtDesc(PageRequest.of(Math.max(page, 0), 10))
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<BlogResponse> feed(long maxTimestamp, int offset) {
        Long userId = currentUser.require().id();
        Set<String> ids = redis.opsForZSet().reverseRangeByScore(
                CacheKeys.FEED + userId, 0, maxTimestamp, offset, 10
        );
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> orderedIds = ids.stream().map(Long::valueOf).toList();
        var blogs = blogRepository.findAllById(orderedIds).stream()
                .collect(java.util.stream.Collectors.toMap(Blog::getId, value -> value));
        return orderedIds.stream().filter(blogs::containsKey).map(id -> response(blogs.get(id))).toList();
    }

    private BlogResponse response(Blog blog) {
        User author = userRepository.findById(blog.getUserId())
                .orElseThrow(() -> new NotFoundException("用户不存在: " + blog.getUserId()));
        AuthenticatedUser viewer = currentUser.optional();
        boolean liked = viewer != null && redis.opsForZSet().score(
                CacheKeys.BLOG_LIKES + blog.getId(), viewer.id().toString()
        ) != null;
        return BlogResponse.from(blog, author, liked);
    }
}
