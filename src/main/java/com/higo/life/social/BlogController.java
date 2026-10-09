package com.higo.life.social;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlogResponse publish(@Valid @RequestBody CreateBlogRequest request) {
        return blogService.publish(request);
    }

    @PutMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleLike(@PathVariable Long id) {
        blogService.toggleLike(id);
    }

    @GetMapping("/hot")
    public List<BlogResponse> hot(@RequestParam(defaultValue = "0") int page) {
        return blogService.hot(page);
    }

    @GetMapping("/feed")
    public List<BlogResponse> feed(
            @RequestParam(defaultValue = "9223372036854775807") long maxTimestamp,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return blogService.feed(maxTimestamp, offset);
    }
}
