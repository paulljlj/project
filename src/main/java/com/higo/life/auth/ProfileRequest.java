package com.higo.life.auth;
import jakarta.validation.constraints.*;
public record ProfileRequest(@NotBlank @Size(max=60) String nickname, @Size(max=500) String icon, @Size(max=500) String bio) {}
