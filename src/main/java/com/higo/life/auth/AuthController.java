package com.higo.life.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final UserRepository users;

    public AuthController(AuthService authService, CurrentUser currentUser,UserRepository users) {
        this.authService = authService;
        this.currentUser = currentUser;this.users=users;
    }

    @PostMapping("/codes")
    public VerificationCodeResponse sendCode(@Valid @RequestBody SendCodeRequest request) {
        return authService.sendCode(request.phone());
    }

    @PostMapping("/sessions")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @DeleteMapping("/sessions/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        currentUser.require();
        authService.logout(SessionInterceptor.bearerToken(request));
    }

    @GetMapping("/me")
    public AuthenticatedUser me() {
        return AuthenticatedUser.from(users.findById(currentUser.require().id()).orElseThrow());
    }

    @GetMapping("/session-help")
    public Map<String, String> sessionHelp() {
        return Map.of("authorization", "Bearer <登录接口返回的 token>");
    }
}
