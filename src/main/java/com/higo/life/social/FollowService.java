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
        if (userId.equals(targetUserId)) {
            throw new ConflictException("不能关注自己");
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new NotFoundException("用户不存在: " + targetUserId);
        }
        if (!followRepository.existsByUserIdAndTargetUserId(userId, targetUserId)) {
            followRepository.save(new Follow(userId, targetUserId));
        }
        redis.opsForSet().add(CacheKeys.FOLLOWING + userId, targetUserId.toString());
    }

    @Transactional
    public void unfollow(Long targetUserId) {
        Long userId = currentUser.require().id();
        followRepository.deleteByUserIdAndTargetUserId(userId, targetUserId);
        redis.opsForSet().remove(CacheKeys.FOLLOWING + userId, targetUserId.toString());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long targetUserId) {
        return followRepository.existsByUserIdAndTargetUserId(currentUser.require().id(), targetUserId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> common(Long otherUserId) {
        Long userId = currentUser.require().id();
        Set<String> ids = redis.opsForSet().intersect(
                CacheKeys.FOLLOWING + userId, CacheKeys.FOLLOWING + otherUserId
        );
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = ids.stream().map(Long::valueOf).toList();
        return userRepository.findAllById(userIds).stream().map(UserSummary::from).toList();
    }
}
