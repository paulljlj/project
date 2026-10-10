package com.higo.life.social;
import com.higo.life.auth.CurrentUser;
import com.higo.life.auth.UserRepository;
import com.higo.life.support.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/api/blogs/{blogId}/comments")
public class CommentController {
    private final BlogCommentRepository comments; private final BlogRepository blogs; private final UserRepository users; private final CurrentUser current;
    public CommentController(BlogCommentRepository comments,BlogRepository blogs,UserRepository users,CurrentUser current) { this.comments=comments;this.blogs=blogs;this.users=users;this.current=current; }
    public record Request(@NotBlank @Size(max=1000) String content) {}
    public record Response(Long id,Long userId,String author,String content,LocalDateTime createdAt) {}
    private Response response(BlogComment c) { return new Response(c.getId(),c.getUserId(),users.findById(c.getUserId()).orElseThrow().getNickname(),c.getContent(),c.getCreatedAt()); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @Transactional
    public Response add(@PathVariable Long blogId,@Valid @RequestBody Request body) {
        Long user=current.require().id(); if(!blogs.existsById(blogId)) throw new NotFoundException("笔记不存在");
        return response(comments.saveAndFlush(new BlogComment(blogId,user,body.content())));
    }
    @GetMapping public List<Response> list(@PathVariable Long blogId,@RequestParam(defaultValue="0") int page) {
        if(page<0 || page>1000) throw new com.higo.life.support.InvalidRequestException("页码不合法");
        return comments.findByBlogIdOrderByIdAsc(blogId,PageRequest.of(page,20)).stream().map(this::response).toList();
    }
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @Transactional
    public void remove(@PathVariable Long blogId,@PathVariable Long id) {
        BlogComment c=comments.findById(id).orElseThrow(()->new NotFoundException("评论不存在"));
        if(!c.getBlogId().equals(blogId) || !c.getUserId().equals(current.require().id())) throw new NotFoundException("评论不存在");
        comments.delete(c);
    }
}
