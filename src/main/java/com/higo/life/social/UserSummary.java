package com.higo.life.social;

import com.higo.life.auth.User;

public record UserSummary(Long id, String nickname, String icon) {

    static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getNickname(), user.getIcon());
    }
}
