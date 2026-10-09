package com.higo.life.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendCodeRequest(@NotBlank @Pattern(regexp = "^1\\d{10}$") String phone) {
}
