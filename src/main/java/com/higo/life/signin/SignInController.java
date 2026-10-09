package com.higo.life.signin;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sign-ins")
public class SignInController {

    private final SignInService signInService;

    public SignInController(SignInService signInService) {
        this.signInService = signInService;
    }

    @PostMapping("/today")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signIn() {
        signInService.signIn();
    }

    @GetMapping("/streak")
    public Map<String, Integer> streak() {
        return Map.of("consecutiveDays", signInService.consecutiveDays());
    }
}
