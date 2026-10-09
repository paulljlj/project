package com.higo.life.auth;

import com.higo.life.support.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final HttpServletRequest request;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    public AuthenticatedUser require() {
        Object user = request.getAttribute(SessionInterceptor.USER_ATTRIBUTE);
        if (user instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw new UnauthorizedException("请先登录");
    }

    public AuthenticatedUser optional() {
        Object user = request.getAttribute(SessionInterceptor.USER_ATTRIBUTE);
        return user instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }
}
