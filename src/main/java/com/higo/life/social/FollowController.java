package com.higo.life.social;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long userId) {
        followService.follow(userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long userId) {
        followService.unfollow(userId);
    }

    @GetMapping("/{userId}")
    public Map<String, Boolean> isFollowing(@PathVariable Long userId) {
        return Map.of("following", followService.isFollowing(userId));
    }

    @GetMapping("/common/{userId}")
    public List<UserSummary> common(@PathVariable Long userId) {
        return followService.common(userId);
    }
}
