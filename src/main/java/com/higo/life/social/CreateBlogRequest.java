package com.higo.life.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBlogRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String content
) {
}
