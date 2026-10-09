package com.higo.life.auth;

public record AuthenticatedUser(Long id, String nickname, String icon) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getNickname(), user.getIcon());
    }
}
