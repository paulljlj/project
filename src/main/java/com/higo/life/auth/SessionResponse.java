package com.higo.life.auth;

public record SessionResponse(String token, AuthenticatedUser user) {
}
