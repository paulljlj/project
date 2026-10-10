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
    private final BlogLikeRepository likes;

    public BlogService(
            BlogRepository blogRepository,
            FollowRepository followRepository,
            UserRepository userRepository,
            StringRedisTemplate redis,
            CurrentUser currentUser, BlogLikeRepository likes
    ) {
        this.blogRepository = blogRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.redis = redis;
        this.currentUser = currentUser; this.likes=likes;
    }

    @Transactional
    public BlogResponse publish(CreateBlogRequest request) {
        AuthenticatedUser author = currentUser.require();
        Blog draft=new Blog(author.id(),request.title(),request.content());
        if(request.images()!=null) draft.setImages(String.join(",",request.images()));
        Blog blog = blogRepository.save(draft);
        long now = System.currentTimeMillis();
        for (Follow follower : followRepository.findByTargetUserId(author.id())) {
            com.higo.life.support.AfterCommit.run(()->redis.opsForZSet().add(CacheKeys.FEED + follower.getUserId(), blog.getId().toString(), now));
        }
        return response(blog);
    }

    @Transactional
    public void toggleLike(Long blogId) {
        Long userId = currentUser.require().id();
        Blog blog=blogRepository.lockById(blogId).orElseThrow(()->new NotFoundException("笔记不存在"));
        var existing=likes.findByBlogIdAndUserId(blogId,userId);
        boolean adding=existing.isEmpty();
        if(adding) likes.save(new BlogLike(blogId,userId)); else likes.delete(existing.get());
        blog.adjustLike(adding?1:-1);
        com.higo.life.support.AfterCommit.run(()->{
            if(adding) redis.opsForZSet().add(CacheKeys.BLOG_LIKES+blogId,userId.toString(),System.currentTimeMillis());
            else redis.opsForZSet().remove(CacheKeys.BLOG_LIKES+blogId,userId.toString());
        });
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
        return orderedIds.stream().filter(blogs::containsKey).filter(id->followRepository.existsByUserIdAndTargetUserId(userId,blogs.get(id).getUserId())).map(id -> response(blogs.get(id))).toList();
    }

    public BlogResponse detail(Long id) { return response(blogRepository.findById(id).orElseThrow(()->new NotFoundException("笔记不存在"))); }
    public List<BlogResponse> byUser(Long id,int page) { return blogRepository.findByUserIdOrderByIdDesc(id,PageRequest.of(Math.max(0,page),10)).stream().map(this::response).toList(); }
    public List<UserSummary> firstLikes(Long id) { return likes.findByBlogIdOrderByCreatedAtAscIdAsc(id,PageRequest.of(0,5)).stream().map(l->UserSummary.from(userRepository.findById(l.getUserId()).orElseThrow())).toList(); }
    public record FeedPage(List<BlogResponse> items,Long nextBeforeId,boolean hasMore) {}
    @Transactional(readOnly=true) public FeedPage cursorFeed(long beforeId) {
        var rows=blogRepository.followingFeed(currentUser.require().id(),beforeId,PageRequest.of(0,11));
        var page=rows.stream().limit(10).toList();
        return new FeedPage(page.stream().map(this::response).toList(),page.isEmpty()?beforeId:page.getLast().getId(),rows.size()>10);
    }
    private BlogResponse response(Blog blog) {
        User author = userRepository.findById(blog.getUserId())
                .orElseThrow(() -> new NotFoundException("用户不存在: " + blog.getUserId()));
        AuthenticatedUser viewer = currentUser.optional();
        boolean liked=viewer!=null && likes.existsByBlogIdAndUserId(blog.getId(),viewer.id());
        return BlogResponse.from(blog, author, liked);
    }
}
