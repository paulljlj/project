package com.higo.life.social;

import com.higo.life.auth.CurrentUser;
import com.higo.life.auth.UserRepository;
import com.higo.life.cache.CacheKeys;
import com.higo.life.support.ConflictException;
import com.higo.life.support.NotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final CurrentUser currentUser;

    public FollowService(
            FollowRepository followRepository,
            UserRepository userRepository,
            StringRedisTemplate redis,
            CurrentUser currentUser
    ) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.redis = redis;
        this.currentUser = currentUser;
    }

    @Transactional
    public void follow(Long targetUserId) {
        Long userId = currentUser.require().id();
        userRepository.lockById(userId).orElseThrow(()->new NotFoundException("用户不存在"));
        if (userId.equals(targetUserId)) {
            throw new ConflictException("不能关注自己");
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new NotFoundException("用户不存在: " + targetUserId);
        }
        if (!followRepository.existsByUserIdAndTargetUserId(userId, targetUserId)) {
            followRepository.save(new Follow(userId, targetUserId));
        }
        com.higo.life.support.AfterCommit.run(()->redis.opsForSet().add(CacheKeys.FOLLOWING + userId, targetUserId.toString()));
    }

    @Transactional
    public void unfollow(Long targetUserId) {
        Long userId = currentUser.require().id();
        userRepository.lockById(userId).orElseThrow(()->new NotFoundException("用户不存在"));
        followRepository.deleteByUserIdAndTargetUserId(userId, targetUserId);
        com.higo.life.support.AfterCommit.run(()->redis.opsForSet().remove(CacheKeys.FOLLOWING + userId, targetUserId.toString()));
    }

    private void snapshot(String key,Long id) {
        String[] values=followRepository.findByUserId(id).stream().map(f->f.getTargetUserId().toString()).toArray(String[]::new);
        if(values.length>0) { redis.opsForSet().add(key,values);redis.expire(key,java.time.Duration.ofSeconds(30)); }
    }
    @Transactional(readOnly = true)
    public boolean isFollowing(Long targetUserId) {
        return followRepository.existsByUserIdAndTargetUserId(currentUser.require().id(), targetUserId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> common(Long otherUserId) {
        Long userId = currentUser.require().id();
        // Isolated snapshots preserve Set-intersection learning without overwriting shared sets.
        String left=CacheKeys.FOLLOWING+"snapshot:"+java.util.UUID.randomUUID();
        String right=CacheKeys.FOLLOWING+"snapshot:"+java.util.UUID.randomUUID();
        Set<String> ids;
        try {
            snapshot(left,userId);snapshot(right,otherUserId);
            ids=redis.opsForSet().intersect(left,right);
        } finally { redis.delete(List.of(left,right)); }
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = ids.stream().map(Long::valueOf).toList();
        return userRepository.findAllById(userIds).stream().map(UserSummary::from).toList();
    }
}
