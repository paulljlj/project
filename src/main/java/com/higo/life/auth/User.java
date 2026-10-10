package com.higo.life.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "nickname", nullable = false, length = 60)
    private String nickname;

    @Column(length = 500)
    private String icon;

    @Column(length = 500)
    private String bio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String phone, String nickname) {
        this.phone = phone;
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void update(String nickname, String icon, String bio) {
        this.nickname = nickname; this.icon = icon; this.bio = bio;
    }
    public String getBio() { return bio; }

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getNickname() {
        return nickname;
    }

    public String getIcon() {
        return icon;
    }
}
