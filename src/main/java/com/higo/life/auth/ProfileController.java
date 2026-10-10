package com.higo.life.auth;
import com.higo.life.support.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@RestController
@RequestMapping("/api/users")
public class ProfileController {
    private final UserRepository users; private final CurrentUser current;
    public ProfileController(UserRepository users, CurrentUser current) { this.users=users; this.current=current; }
    public record Profile(Long id,String nickname,String icon,String bio) {
        static Profile from(User u) { return new Profile(u.getId(),u.getNickname(),u.getIcon(),u.getBio()); }
    }
    @GetMapping("/{id}") public Profile get(@PathVariable Long id) { return Profile.from(users.findById(id).orElseThrow(()->new NotFoundException("用户不存在"))); }
    @PutMapping("/me") @Transactional public Profile update(@Valid @RequestBody ProfileRequest body) {
        User u=users.findById(current.require().id()).orElseThrow(()->new NotFoundException("用户不存在"));
        if(body.icon()!=null && !body.icon().isBlank() && !body.icon().matches("/api/uploads/[a-f0-9-]+\\.(png|jpg|gif)")) throw new com.higo.life.support.InvalidRequestException("头像必须使用本站上传的图片");
        u.update(body.nickname(),body.icon(),body.bio()); return Profile.from(u);
    }
}
